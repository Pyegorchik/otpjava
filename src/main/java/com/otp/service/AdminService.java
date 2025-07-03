package com.otp.service;

import java.sql.SQLException;
import java.util.List;

import com.otp.dao.OtpCodeDao;
import com.otp.dao.OtpConfigDao;
import com.otp.dao.UserDao;
import com.otp.model.OtpConfig;
import com.otp.model.User;

public class AdminService {
    private final UserDao userDao;
    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;

    public AdminService(UserDao userDao, OtpConfigDao otpConfigDao, OtpCodeDao OtpCodeDao) {
        this.userDao = userDao;
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = OtpCodeDao;
    }

    public OtpConfig getOtpConfig() throws SQLException {
        try {
            return otpConfigDao.getConfig();
        } catch (SQLException e) {
            throw e;
        }
    }

    public void updateOtpConfig(int codeLength, int expiryMinutes) throws SQLException {
        try {
            otpConfigDao.updateConfig(codeLength, expiryMinutes);
        } catch (SQLException e) {
            throw e;
        }
    }
       

    public List<User> getAllUsers() throws SQLException {
        return userDao.findAllUsers(); // Метод уже возвращает только USER'ов
    }

    public void deleteUser(Long userId) throws SQLException {
        otpCodeDao.deleteByUserId(userId);
        userDao.delete(userId);
    }

    
}