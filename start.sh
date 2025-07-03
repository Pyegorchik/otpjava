#!/bin/bash

echo "🚀 Starting OTP Service..."

#!/bin/bash

# Конфигурация
DB_NAME="otpservice"
DB_USER="otpadmin"
DB_PASSWORD="admin123"
DB_HOST="localhost"
DB_PORT="5432"
INIT_SQL_FILE="src/main/resources/init.sql"  # Путь к SQL-файлу

# Функция для вывода ошибок
error_exit() {
    echo "❌ Ошибка: $1" >&2
    exit 1
}

# Проверка PostgreSQL
check_postgres() {
    if ! command -v psql &> /dev/null; then
        error_exit "PostgreSQL не установлен. Установите его командой: sudo apt-get install postgresql"
    fi

    if ! pg_isready -h $DB_HOST -p $DB_PORT &> /dev/null; then
        echo "🔄 PostgreSQL не запущен, пытаемся запустить..."
        sudo service postgresql start || error_exit "Не удалось запустить PostgreSQL"
        sleep 2
    fi
}

# Настройка базы данных
setup_database() {
    echo "🔧 Настраиваем базу данных..."
    
    # Выполняем все команды под пользователем postgres
    sudo -u postgres psql <<EOF 2>/dev/null
CREATE USER $DB_USER WITH SUPERUSER CREATEDB CREATEROLE PASSWORD '$DB_PASSWORD';
CREATE DATABASE $DB_NAME OWNER $DB_USER;
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
EOF

    # Проверяем успешность выполнения
    if [ $? -ne 0 ]; then
        echo "⚠️ Пользователь или база данных уже существуют, продолжаем..."
    fi
}

# Инициализация таблиц
init_tables() {
    echo "🗄️ Создаем таблицы из файла $INIT_SQL_FILE..."
    
    if [ ! -f "$INIT_SQL_FILE" ]; then
        error_exit "SQL-файл инициализации не найден: $INIT_SQL_FILE"
    fi

    # Используем PGPASSWORD для автоматической аутентификации
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$INIT_SQL_FILE" \
        || error_exit "Не удалось создать таблицы"
}

# Основной скрипт
echo "🚀 Запуск инициализации базы данных OTP сервиса"

check_postgres
setup_database
init_tables

echo "✅ Инициализация базы данных завершена успешно!"
echo "🔗 Данные для подключения: postgresql://$DB_USER:*****@$DB_HOST:$DB_PORT/$DB_NAME"

# Запуск приложения
echo "🔧 Сборка проекта..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "✅ Сборка успешна. Запуск сервиса..."
    ./gradlew run
else
    echo "❌ Ошибка сборки"
    exit 1
fi