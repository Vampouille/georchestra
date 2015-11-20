package org.georchestra.ldapadmin.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "ldapadmin.user_token")
public class UserToken {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private String uid;
    private String token;
    private Date creationDate;

    public UserToken() {}

    public UserToken(String uid, String token) {
        this.uid = uid;
        this.token = token;
        this.creationDate = new Date();
    }

    public UserToken(String uid, String token, Date creationDate) {
        this.uid = uid;
        this.token = token;
        this.creationDate = creationDate;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

}
