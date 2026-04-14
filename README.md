Lottery-Engine
高并发可配置营销抽奖引擎，适配大流量活动场景，核心接口低延迟、高可用。
Tech Stack（技术栈）

SpringBoot + MySQL + MyBatis + Redis + RabbitMQ + XXL-JOB + Db-Router + AOP

Core Features（核心功能）

- 活动、奖品规则动态配置

- Redis原子操作防超卖，保证抽奖公平性

- 分布式限流、黑名单防刷

- MQ异步发奖，提升接口响应速度

- 分库分表适配大数据量，支持水平扩展

- 定时对账、过期活动清理

Performance（性能）
核心抽奖接口响应：几十毫秒级 | 支持高QPS压测，可水平扩展

Quick Start（快速启动）

1. 执行SQL脚本，初始化表结构

2. 配置Redis、RabbitMQ、MySQL连接信息

3. 启动SpringBoot应用，即可调用接口测试

Project Structure（项目结构）

controller：接口层 | domain：业务逻辑 | dao/mapper：数据访问 | aop：限流切面 | common：通用工具 | config：中间件配置