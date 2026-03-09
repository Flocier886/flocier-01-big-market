package com.flocier.domain.strategy.service.rule.chain;


import com.flocier.domain.strategy.repository.IStrategyRepository;

import javax.annotation.Resource;

public abstract class AbstractLogicChain implements ILogicChain{
    @Resource
    protected IStrategyRepository repository;

    private ILogicChain next;

    @Override
    public ILogicChain next() {
        return this.next;
    }

    @Override
    public ILogicChain appendNext(ILogicChain next) {
        this.next=next;
        return this.next;
    }

    protected abstract String ruleModel();

    protected String awardValue(Integer awardId){
        return repository.queryStrategyAwardValue(awardId);
    }
}
