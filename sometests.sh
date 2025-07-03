#!/bin/bash

# Конфигурация
BASE_URL="http://localhost:8080"
OTP_FILE="otp-codes.txt"
ADMIN_USER="admin_$(date +%s)"
REGULAR_USER="user_$(date +%s)"
PASSWORD="strongPassword123!"

# Утилиты
JQ_INSTALLED=$(command -v jq)
CURL_INSTALLED=$(command -v curl)

# Проверка зависимостей
if [ -z "$CURL_INSTALLED" ]; then
    echo "Ошибка: curl не установлен"
    exit 1
fi

if [ -z "$JQ_INSTALLED" ]; then
    echo "Предупреждение: jq не установлен, некоторые функции будут ограничены"
fi

# Функции API
register() {
    local username=$1
    local password=$2
    local role=$3
    
    response=$(curl -s -X POST "$BASE_URL/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\",\"role\":\"$role\"}")
    
    if [ -n "$JQ_INSTALLED" ]; then
        echo "$response" | jq
    else
        echo "$response"
    fi
}

login() {
    local username=$1
    local password=$2
    
    response=$(curl -s -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    
    if [ -n "$JQ_INSTALLED" ]; then
        echo "$response" | jq -r '.token'
    else
        echo "$response" | grep -o '"token":"[^"]*' | grep -o '[^"]*$'
    fi
}

generate_otp() {
    local token=$1
    local operation_id=$2
    
    response=$(curl -s -X POST "$BASE_URL/api/user/otp/generate" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"operationId\":\"$operation_id\",\"deliveryMethod\":\"FILE\"}")
    
    if [ -n "$JQ_INSTALLED" ]; then
        echo "$response" | jq
    else
        echo "$response"
    fi
}

validate_otp() {
    local token=$1
    local operation_id=$2
    local otp_code=$3
    
    response=$(curl -s -X POST "$BASE_URL/api/user/otp/validate" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d "{\"operationId\":\"$operation_id\",\"code\":\"$otp_code\"}")
    
    if [ -n "$JQ_INSTALLED" ]; then
        echo "$response" | jq
    else
        echo "$response"
    fi
}

# Сценарии
full_cycle_scenario() {
    echo "=== Начало полного сценария: Регистрация -> Авторизация -> Генерация OTP -> Валидация ==="
    
    # 1. Регистрация пользователя
    echo "Регистрация пользователя: $REGULAR_USER"
    register_response=$(register "$REGULAR_USER" "$PASSWORD" "USER")
    echo "Ответ регистрации: $register_response"
    
    # 2. Авторизация
    echo "Авторизация пользователя: $REGULAR_USER"
    token=$(login "$REGULAR_USER" "$PASSWORD")
    echo "Полученный токен: $token"
    
    # 3. Генерация OTP
    operation_id="op_$(date +%s)"
    echo "Генерация OTP для операции: $operation_id"
    generate_response=$(generate_otp "$token" "$operation_id")
    echo "Ответ генерации OTP: $generate_response"
    
    # 4. Поиск OTP в файле
    echo "Поиск OTP в файле $OTP_FILE..."
    sleep 3 # Даем время на запись в файл
    
    if [ ! -f "$OTP_FILE" ]; then
        echo "Ошибка: Файл $OTP_FILE не найден"
        return 1
    fi
    
    # Ищем последнюю запись для нашего пользователя
    echo "egister_response $register_response"
    user_id=$(echo "$register_response" | grep -o '"id": *[^,}]*' | awk -F': ' '{print $2}' | tr -d '"')
    echo "user_id $user_id"
    otp_record=$(grep "^$user_id," "$OTP_FILE" | tail -n1)
    
    if [ -z "$otp_record" ]; then
        echo "Ошибка: OTP код не найден в файле"
        return 1
    fi
    
    echo "Найдена запись OTP: $otp_record"
    otp_code=$(echo "$otp_record" | cut -d',' -f2)
    
    # 5. Валидация OTP
    echo "Валидация OTP кода: $otp_code"
    validate_response=$(validate_otp "$token" "$operation_id" "$otp_code")
    echo "Ответ валидации: $validate_response"
    
    echo "=== Полный сценарий завершен ==="
}

admin_registration_scenario() {
    echo "=== Сценарий регистрации администратора ==="
    
    # Первый администратор (должен успешно зарегистрироваться)
    echo "Попытка регистрации первого администратора"
    response1=$(register "$ADMIN_USER" "$PASSWORD" "ADMIN")
    echo "Первый администратор: $response1"
    
    # Второй администратор (должен получить ошибку)
    echo "Попытка регистрации второго администратора"
    response2=$(register "${ADMIN_USER}_2" "$PASSWORD" "ADMIN")
    echo "Второй администратор: $response2"
    
    echo "=== Сценарий завершен ==="
}

generate_otp_scenario() {
    echo "=== Сценарий генерации OTP ==="
    
    # Создаем временного пользователя
    temp_user="temp_user_$(date +%s)"
    register "$temp_user" "$PASSWORD" "USER" > /dev/null
    token=$(login "$temp_user" "$PASSWORD")
    
    # Успешная генерация
    echo "Успешная генерация OTP:"
    generate_otp "$token" "valid_op_$(date +%s)"
    
    # Неверный метод доставки
    echo "Генерация с неверным методом доставки:"
    response=$(curl -s -X POST "$BASE_URL/api/user/otp/generate" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $token" \
        -d '{"operationId":"invalid_method_op","deliveryMethod":"INVALID"}')
    echo "$response"
    
    echo "=== Сценарий завершен ==="
}

# Главное меню
while true; do
    echo ""
    echo "Выберите сценарий:"
    echo "1) Полный цикл (регистрация, авторизация, генерация OTP, валидация)"
    echo "2) Регистрация администратора"
    echo "3) Тестирование генерации OTP"
    echo "4) Выход"
    read -p "Ваш выбор: " choice
    
    case $choice in
        1) full_cycle_scenario ;;
        2) admin_registration_scenario ;;
        3) generate_otp_scenario ;;
        4) exit 0 ;;
        *) echo "Неверный выбор, попробуйте снова" ;;
    esac
done