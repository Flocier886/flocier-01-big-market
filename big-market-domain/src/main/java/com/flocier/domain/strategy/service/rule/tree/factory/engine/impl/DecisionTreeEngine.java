package com.flocier.domain.strategy.service.rule.tree.factory.engine.impl;

import com.flocier.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.flocier.domain.strategy.model.vo.RuleTreeNodeLineVO;
import com.flocier.domain.strategy.model.vo.RuleTreeNodeVO;
import com.flocier.domain.strategy.model.vo.RuleTreeVO;
import com.flocier.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.flocier.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.flocier.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {
    private final Map<String, ILogicTreeNode>logicTreeNodeMap;
    private final RuleTreeVO ruleTreeVO;

    public DecisionTreeEngine(Map<String, ILogicTreeNode> logicTreeNodeMap, RuleTreeVO ruleTreeVO) {
        this.logicTreeNodeMap = logicTreeNodeMap;
        this.ruleTreeVO = ruleTreeVO;
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId) {
        DefaultTreeFactory.StrategyAwardVO strategyAwardData=null;
        //获取决策树的头结点
        String nextNode=ruleTreeVO.getTreeRootRuleNode();
        Map<String, RuleTreeNodeVO>treeNodeVOMap=ruleTreeVO.getTreeNodeMap();
        //判断是否为空，为空表明这个节点就是结果
        RuleTreeNodeVO ruleTreeNode=treeNodeVOMap.get(nextNode);
        //遍历节点并根据节点运行的结果访问下一个节点
        while(nextNode!=null){
            //获取决策节点
            ILogicTreeNode treeNode=logicTreeNodeMap.get(ruleTreeNode.getRuleKey());
            String ruleValue=ruleTreeNode.getRuleValue();
            DefaultTreeFactory.TreeActionEntity actionEntity=treeNode.logic(userId,strategyId,awardId,ruleValue);
            RuleLogicCheckTypeVO logicCheckTypeVO=actionEntity.getRuleLogicCheckType();
            strategyAwardData=actionEntity.getStrategyAwardVO();
            log.info("决策树引擎【{}】treeId:{} node:{} code:{}", ruleTreeVO.getTreeName(), ruleTreeVO.getTreeId(), nextNode, logicCheckTypeVO.getCode());
            //获取下一个节点
            nextNode=nextNode(logicCheckTypeVO.getCode(),ruleTreeNode.getTreeNodeLineVOList());
            ruleTreeNode=treeNodeVOMap.get(nextNode);
        }
        //返回结果
        return strategyAwardData;
    }

    private String nextNode(String matterValue, List<RuleTreeNodeLineVO> treeNodeLineVOList) {
        if(treeNodeLineVOList==null ||treeNodeLineVOList.isEmpty())return null;
        for(RuleTreeNodeLineVO ruleTreeNodeLineVO:treeNodeLineVOList){
            if(decisionLogic(matterValue,ruleTreeNodeLineVO)){
                return ruleTreeNodeLineVO.getRuleNodeTo();
            }
        }
        throw new RuntimeException("决策树引擎，nextNode 计算失败，未找到可执行节点！");
    }

    private boolean decisionLogic(String matterValue, RuleTreeNodeLineVO ruleTreeNodeLineVO) {
        switch (ruleTreeNodeLineVO.getRuleLimitType()){
            case EQUAL:
                return matterValue.equals(ruleTreeNodeLineVO.getRuleLimitValue().getCode());
            // 以下规则暂时不需要实现
            case GT:
            case LT:
            case GE:
            case LE:
            default:
                return false;

        }
    }
}
