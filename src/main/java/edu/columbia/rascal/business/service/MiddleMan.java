package edu.columbia.rascal.business.service;

import edu.columbia.rascal.business.service.review.iacuc.IacucTaskForm;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cmd.AbstractCustomSqlExecution;
import org.activiti.engine.impl.cmd.CustomSqlExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class MiddleMan {

    private static final Logger log = LoggerFactory.getLogger(MiddleMan.class);
    private final JdbcTemplate jdbcTemplate;

    @Resource
    private ManagementService managementService;
    @Resource
    private IacucProcessService processService;
    @Resource
    private RuntimeService runtimeService;

    @Resource
    private IacucProtocolHeaderService headerService;

    @Autowired
    public MiddleMan(JdbcTemplate jt) {
        this.jdbcTemplate = jt;
    }


    public void suspendProtocol(String protocolId) {
        headerService.suspendProtocolByHeaderId(protocolId);
    }

    public void activateProtocol(String protocolId) {
        headerService.activateProtocolByHeaderId(protocolId);
    }

    private boolean submitProtocol(String protocolId, String userId) {
        if (processService.isProtocolProcessStarted(protocolId)) {
            return false;
        } else {
            return startProcess(protocolId, userId);
        }
    }

    private boolean startProcess(String protocolId, String userId) {
        Map<String, Object> map = new HashMap<String, Object>();
        return processService.startProtocolProcess(protocolId, userId, map);
    }



    public boolean hasTask(String protocolId, String taskDefKey) {
        return processService.hasTaskByTaskDefKey(protocolId, taskDefKey);
    }



    public void deleteProcessByBizKey(String protocolId, String deletedReason) {
        log.info("delete process: bizKey={}, deleteReason={}", protocolId, deletedReason);
        runtimeService.deleteProcessInstance(protocolId, deletedReason);
    }

    public void foo() {
        CustomSqlExecution<IacucMybatisMapper, List<Map<String, Object>>>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        List<Map<String, Object>>>(IacucMybatisMapper.class) {

                    public List<Map<String, Object>> execute(IacucMybatisMapper
                                                                     customMapper) {
                        return customMapper.selectTasks();
                    }
                };

        List<Map<String, Object>> results =
                managementService.executeCustomSql(sqlExecution);
        for (Map<String, Object> m : results) {
            for (String k : m.keySet()) {
                log.info("key==================" + k);
            }
        }
    }


    public void foo2() {
        CustomSqlExecution<IacucMybatisMapper, List<Map<String, Object>>>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        List<Map<String, Object>>>(IacucMybatisMapper.class) {

                    public List<Map<String, Object>> execute(IacucMybatisMapper customMapper) {
                        return customMapper.selectTaskWithSpecificVariable("PROTOCOL_ID");
                    }
                };

        List<Map<String, Object>> results = managementService.executeCustomSql(sqlExecution);
        for (Map<String, Object> m : results) {
            for (String k : m.keySet()) {
                log.info("Foo key=" + k);
            }
        }
    }

    /**
     * @param taskId  final String
     * @param endTime final Date
     * @return boolean
     */
    private boolean updateEndTimeByTaskId(final String taskId, final Date endTime) {
        CustomSqlExecution<IacucMybatisMapper, Boolean>
                sqlExecution =
                new AbstractCustomSqlExecution<IacucMybatisMapper,
                        Boolean>(IacucMybatisMapper.class) {
                    @Override
                    public Boolean execute(IacucMybatisMapper cmapper) {
                        cmapper.updateEndTime(taskId, endTime);
                        return true;
                    }
                };
        return managementService.executeCustomSql(sqlExecution);
    }

    public List<IacucTaskForm> getIacucProtocolHistory(String protocolId) {
        List<IacucTaskForm> list = new ArrayList<IacucTaskForm>();
        list = processService.getIacucProtocolHistory(protocolId);
        for (IacucTaskForm hs : list) {
            log.info(hs.toString());
        }
        return list;
    }

    public void releaseApproval(DelegateExecution execution) {
        log.info("release approval task: protocolId=bizKey=" + execution.getProcessBusinessKey());
    }

}
