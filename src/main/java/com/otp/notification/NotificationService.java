package com.otp.notification;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.otp.dao.UserDao;
import com.otp.model.OtpCode;
import com.otp.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.Address;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final UserDao userDao;
    private final EmailService emailService;
    private final SmsService smsService;
    private final TelegramService telegramService;
    private final FileService fileService;
    
    public NotificationService(UserDao userDao, Properties config) {
        this.userDao = userDao;
        this.emailService = new EmailService(config);
        this.smsService = new SmsService(config);
        this.telegramService = new TelegramService(config);
        this.fileService = new FileService(config);
    }
    
    public void sendOtp(Long userId, String code, OtpCode.DeliveryMethod method) {
        try {
            User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            switch (method) {
                case SMS:
                    smsService.sendSms(user.getPhone(), "Your OTP code: " + code);
                    break;
                case EMAIL:
                    emailService.sendEmail(user.getEmail(), "OTP Code", "Your OTP code: " + code);
                    break;
                case TELEGRAM:
                    telegramService.sendMessage(user.getTelegramChatId(), "Your OTP code: " + code);
                    break;
                case FILE:
                    fileService.saveToFile(userId, code);
                    break;
            }
            
            logger.info("OTP sent via {} to user {}", method, userId);
        } catch (Exception e) {
            logger.error("Failed to send OTP via {} to user {}", method, userId, e);
            throw new RuntimeException("Failed to send OTP", e);
        }
    }
    
    public static class EmailService {
        private final  javax.mail.Session session;
        private final String fromEmail;
        
        public EmailService(Properties config) {
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getProperty("email.smtp.host"));
            props.put("mail.smtp.port", config.getProperty("email.smtp.port"));
            props.put("mail.smtp.auth", config.getProperty("email.smtp.auth"));
            props.put("mail.smtp.starttls.enable", config.getProperty("email.smtp.starttls"));
            
            this.fromEmail = config.getProperty("email.smtp.username");
            String password = config.getProperty("email.smtp.password");
            
            this.session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            });
        }
        
        public void sendEmail(String toEmail, String subject, String text) {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setText(text);
                
                Transport.send(message);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email", e);
            }
        }
    }
    
public static class SmsService {
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    public SmsService(Properties config) {
        this.host = config.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(config.getProperty("smpp.port", "2775"));
        this.systemId = config.getProperty("smpp.system_id", "smppclient1");
        this.password = config.getProperty("smpp.password", "password");
        this.systemType = config.getProperty("smpp.system_type", "OTP");
        this.sourceAddr = config.getProperty("smpp.source_addr", "OTPService");
    }

    public void sendSms(String destination, String code) {
        org.smpp.Session session = null;
        try {
            // 1. Создание соединения
            TCPIPConnection connection = new TCPIPConnection(host, port);
            connection.setReceiveTimeout(20_000);
            
            // 2. Создание сессии
            session = new org.smpp.Session(connection);
            
            // 3. Настройка Bind запроса
            BindRequest bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34); // SMPP v3.4
            
            // 4. Выполнение привязки
            BindResponse bindResponse = session.bind(bindRequest);
            
  
            // 5. Подготовка SMS
            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(new Address( (byte) 0, (byte) 1, sourceAddr));
            submitSM.setDestAddr(new Address((byte) 0, (byte) 1, destination));  
            submitSM.setShortMessage("Your code: " + code);       
            submitSM.setDataCoding((byte) 0);                     
            
            // 6. Отправка SMS
            SubmitSMResp submitResp = session.submit(submitSM);
            
            if (submitResp.getCommandStatus() != 0) {
                throw new RuntimeException("SubmitSM failed. Status: " + submitResp.getCommandStatus());
            }
            
            System.out.println("SMS sent successfully. Message ID: " + submitResp.getMessageId());
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMS", e);
        } finally {
            if (session != null) {
                try {
                    session.unbind();
                    session.close();
                } catch (Exception e) {
                    System.err.println("Error while closing session: " + e.getMessage());
                }
            }
        }
    }
}
    
    public static class TelegramService {
        private final String botToken;
        private final String apiUrl = "https://api.telegram.org/bot";
        
        public TelegramService(Properties config) {
            this.botToken = config.getProperty("telegram.bot.token");
        }
        
        public void sendMessage(Long chatId, String message) {
            if (chatId == null) {
                throw new IllegalArgumentException("Telegram chat ID not set for user");
            }
            
            try {
                String url = String.format("%s%s/sendMessage?chat_id=%d&text=%s",
                    apiUrl, botToken, chatId, URLEncoder.encode(message, StandardCharsets.UTF_8));
                
                // Простая реализация HTTP-запроса (можно заменить на HttpClient)
                new java.net.URL(url).openStream().close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to send Telegram message", e);
            }
        }
    }
    
    public static class FileService {
        private final Path storagePath;
        
        public FileService(Properties config) {
            this.storagePath = Paths.get(config.getProperty("file.storage.path"));
        }
        
        public void saveToFile(Long userId, String code) {
            try {
                String line = String.format("%s,%s,%s%n", userId, code, java.time.LocalDateTime.now());
                Files.write(storagePath, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save OTP to file", e);
            }
        }
    }
}