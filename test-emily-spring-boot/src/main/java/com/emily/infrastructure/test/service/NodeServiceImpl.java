package com.emily.infrastructure.test.service;

import com.emily.infrastructure.datasource.annotation.TargetDataSource;
import com.emily.infrastructure.test.mapper.MysqlMapper;
import com.emily.infrastructure.test.mapper.SlaveMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * @program: spring-parent
 * @description:
 * @author: Emily
 * @create: 2021/05/13
 */
@Service
public class NodeServiceImpl implements NodeService{
    @Autowired
    private SlaveMapper slaveMapper;
    @Autowired
    private MysqlMapper mysqlMapper;
    @Autowired
    private MysqlService mysqlService;

    @Override
    //@Transactional(rollbackFor = Exception.class)
    public void findNode() throws Exception {
       // Long eid = slaveMapper.findNode();
        mysqlService.insertMysql();
       // nodeService.instertStatus();
        //mysqlMapper.insertLocks(System.currentTimeMillis()+"", Math.random()+"");
        //slaveMapper.insertStatus();
        //mysqlMapper.findLocks("TEST2");
        //nodeMapper.findNode();
       // insertMysql();

    }

    @TargetDataSource(value = "slave")
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager", isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    @Override
    public void instertStatus() {
        slaveMapper.insertStatus();
    }

    @TargetDataSource(value = "mysql")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insertMysql() throws IOException {
        for (int i=0; i<2;i++){
            mysqlMapper.insertLocks("name"+i, "lock"+i);
            String lockName = mysqlMapper.findLocks("lock"+i);
            System.out.println("==>》查询到的lock名称是："+lockName);
            System.out.println();
            mysqlMapper.delLocks(lockName);
            System.out.println("==>》删除数据成功==>"+lockName);
        }
        mysqlMapper.insertLocks(System.currentTimeMillis()+"", Math.random()+"");
        throw new IOException();
    }
}
