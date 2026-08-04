package com.example.sbcore.service;

import com.example.sbcore.cache.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CacheHitHelperService {

    @Autowired
    private BookRepository bookRepository;

    @Cacheable(value = "hit", key = "#isbn")
    public String getWithCache(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }
}
