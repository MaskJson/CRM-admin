package com.moving.admin.dao.natives;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.internal.NativeQueryImpl;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectPageNative extends AbstractNative {

    private StringBuilder select = new StringBuilder("select a.id as id, a.create_user_id as createUserId, a.name as name, a.customer_id as customerId, c.name as customerName, u.nick_name as createUser, a.follow, a.create_time as createTime");
    private StringBuilder selectCount = new StringBuilder("select count(1)");
    private StringBuilder from = new StringBuilder(" from project a left join customer c on c.id=a.customer_id left join sys_user u on a.create_user_id=u.id");
    private StringBuilder where = new StringBuilder(" where 1=1");
    private StringBuilder sort = new StringBuilder(" order by ");

    public Map<String, Object> getResult(EntityManager entityManager) {
        Map<String, Object> map = new HashMap<>();
        map.put("content", getProjectList(entityManager));
        map.put("totalElements", getProjectTotalElements(entityManager));
        return map;
    }

    public List<Map<String, Object>> getProjectList(EntityManager entityManager) {
        String sql = select.append(from).append(where).append(sort).toString();
        Session session = entityManager.unwrap(Session.class);
        NativeQuery<Map<String, Object>> query = session.createNativeQuery(sql);
        query.addScalar("id", StandardBasicTypes.LONG);
        query.addScalar("createUserId", StandardBasicTypes.LONG);
        query.addScalar("customerName", StandardBasicTypes.STRING);
        query.addScalar("name", StandardBasicTypes.STRING);
        query.addScalar("follow", StandardBasicTypes.BOOLEAN);
        query.addScalar("createUser", StandardBasicTypes.STRING);
        query.addScalar("createTime", StandardBasicTypes.TIMESTAMP);
        query.unwrap(NativeQueryImpl.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        return query.getResultList();
    }

    public BigInteger getProjectTotalElements(EntityManager entityManager) {
        String sqlCount = selectCount.append(from).append(where).toString();
        Query query = entityManager.createNativeQuery(sqlCount);
        return objectToBigInteger(query.getSingleResult());
    }

    public void filterUserIdIsInTeam(Long userId) {
        // 创建者可看，一周开放：主顾问和顾问可看
        String filter = " and (a.create_user_id=" + userId +
                " or  a.advise_id="+userId +
                " or "+userId+" in(select pa.user_id from project_adviser pa where pa.project_id=a.id)"+
                " or (" + userId + " in (select ttt.user_id from team ttt where ttt.team_id=a.team_id and ttt.team_id is not null) " +
                "and (a.open_type<>1 or (a.open_type=1 and date_add(a.create_time, interval 7 day) < now()))))";
        where.append(filter);
    }

    public void setFolder(String folderWhere) {
        where.append(" and " + folderWhere);
    }

    public void setCustomer(Long customerId) {
        where.append(" and a.customer_id=" + customerId);
    }

    public void setTeam(Long teamId) {
        where.append(" and a.team_id=" + teamId);
    }

    public void setIndustry(String industry) {
        where.append(" and a.industry like '%" + industry + "%'");
    }

    public void setCity(String city) {
        where.append(" and a.city='" + city + "'");
    }

    public void setFollow(Boolean follow) { where.append(" and a.follow=" + (follow ? 1 : 0)); }

    public void setStatus(Integer status) {
        String w = status != 2 ? " and a.status=" + status : " and (a.status=2 or a.status=3)";
        where.append(w);
    }

    public void appendSort(Pageable pageable) {
        super.simpleAppendSort(pageable, sort);
    }
}
