package com.alok.monthlydashboard.service.impl;

import com.alok.monthlydashboard.service.AppDateProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SystemAppDateProvider implements AppDateProvider {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }
}
