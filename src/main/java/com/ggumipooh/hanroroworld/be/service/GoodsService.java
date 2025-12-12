package com.ggumipooh.hanroroworld.be.service;

import com.ggumipooh.hanroroworld.be.model.Goods;
import com.ggumipooh.hanroroworld.be.Repository.GoodsRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsRepository goodsRepository;

    public List<Goods> getAll() {
        return goodsRepository.findAll();
    }

    public Goods getById(long id) {
        return goodsRepository.findById(id).orElse(null);
    }
}
