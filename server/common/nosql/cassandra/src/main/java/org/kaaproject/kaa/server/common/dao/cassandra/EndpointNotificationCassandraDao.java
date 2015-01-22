package org.kaaproject.kaa.server.common.dao.cassandra;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Select;
import org.kaaproject.kaa.common.dto.EndpointNotificationDto;
import org.kaaproject.kaa.server.common.dao.cassandra.filter.CassandraEPByAppIdDao;
import org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraEndpointNotification;
import org.kaaproject.kaa.server.common.dao.impl.EndpointNotificationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.kaaproject.kaa.server.common.dao.cassandra.CassandraDaoUtil.getByteBuffer;
import static org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraModelConstants.ET_NF_COLUMN_FAMILY_NAME;
import static org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraModelConstants.ET_NF_ENDPOINT_KEY_HASH_PROPERTY;
import static org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraModelConstants.ET_NF_SEQ_NUM_PROPERTY;
import static org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraModelConstants.NF_BY_APP_APPLICATION_ID_PROPERTY;
import static org.kaaproject.kaa.server.common.dao.cassandra.model.CassandraModelConstants.NF_BY_APP_COLUMN_FAMILY_NAME;

@Repository
public class EndpointNotificationCassandraDao extends AbstractCassandraDao<CassandraEndpointNotification, String> implements EndpointNotificationDao<CassandraEndpointNotification> {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointNotificationCassandraDao.class);

    @Autowired
    private CassandraEPByAppIdDao cassandraEPByAppIdDao;

    @Override
    protected Class<CassandraEndpointNotification> getColumnFamilyClass() {
        return CassandraEndpointNotification.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ET_NF_COLUMN_FAMILY_NAME;
    }

    @Override
    public List<CassandraEndpointNotification> findNotificationsByKeyHash(byte[] keyHash) {
        LOG.debug("Find endpoint notifications by endpoint key hash {}", keyHash);
        List<CassandraEndpointNotification> cassandraEndpointNotifications = Collections.emptyList();
        if (keyHash != null) {
            Select.Where where = select().from(getColumnFamilyName()).where(eq(ET_NF_ENDPOINT_KEY_HASH_PROPERTY, getByteBuffer(keyHash)));
            LOG.debug("Execute query {}:", where);
            cassandraEndpointNotifications = findListByStatement(where);
        }
        return cassandraEndpointNotifications;
    }

    @Override
    public void removeNotificationsByKeyHash(byte[] keyHash) {
        LOG.debug("Remove endpoint notifications by endpoint key hash {}", keyHash);
        execute(delete().from(getColumnFamilyName()).where(eq(ET_NF_ENDPOINT_KEY_HASH_PROPERTY, getByteBuffer(keyHash))));
    }

    @Override
    public void removeNotificationsByAppId(String appId) {
        LOG.debug("Remove endpoint notifications by app id {}", appId);
        Statement deleteEPNfs = delete().from(getColumnFamilyName()).where(in(ET_NF_ENDPOINT_KEY_HASH_PROPERTY, cassandraEPByAppIdDao.getEPIdsListByAppId(appId)));
        Statement deleteEPNfsByAppId = delete().from(NF_BY_APP_COLUMN_FAMILY_NAME).where(eq(NF_BY_APP_APPLICATION_ID_PROPERTY, appId));
        executeBatch(BatchStatement.Type.UNLOGGED, deleteEPNfs, deleteEPNfsByAppId);
    }

    @Override
    public CassandraEndpointNotification save(EndpointNotificationDto dto) {
        CassandraEndpointNotification endpointNotification = new CassandraEndpointNotification(dto);
        LOG.debug("Save endpoint notification for endpoint profile {}", endpointNotification.getEndpointKeyHash());
        save(new CassandraEndpointNotification(dto));
        LOG.trace("Saved endpoint notification {}", endpointNotification);
        return endpointNotification;
    }

    @Override
    public CassandraEndpointNotification findById(String id) {
        LOG.debug("Try to find endpoint notifications by id {}", id);
        CassandraEndpointNotification key = new CassandraEndpointNotification(id);
        Select.Where where = select().from(getColumnFamilyName()).where(eq(ET_NF_ENDPOINT_KEY_HASH_PROPERTY, key.getEndpointKeyHash()))
                .and(eq(ET_NF_SEQ_NUM_PROPERTY, key.getSeqNum()));
        LOG.debug("[id] Execute query {}:", id, where);
        CassandraEndpointNotification endpointNotification = findOneByStatement(where);
        LOG.debug(" endpoint notification by id {}:", id, endpointNotification != null ? "Found" : "No found");
        LOG.trace("Found endpoint notification {} by id {}:", endpointNotification, id);
        return endpointNotification;
    }
}
