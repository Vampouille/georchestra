package org.georchestra.ldapadmin.dao;

import org.georchestra.ldapadmin.model.UserToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface UserTokenDao extends CrudRepository<UserToken, String> {

    public List<UserToken> findByBeforeCreationDate(Date date);

    public UserToken findOneByToken(String token);

    public UserToken findOneByUid(String uid);
}
