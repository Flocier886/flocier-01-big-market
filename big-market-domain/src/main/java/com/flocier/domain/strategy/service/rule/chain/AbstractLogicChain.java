package com.flocier.domain.strategy.service.rule.chain;


public abstract class AbstractLogicChain implements ILogicChain{

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
}
