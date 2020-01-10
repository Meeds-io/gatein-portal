package org.exoplatform.web.security.jpa;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "GateInToken")
@ExoEntity
@Table(name = "PORTAL_TOKENS")
@NamedQueries({
        @NamedQuery(name = "GateInToken.findByTokenId", query = "SELECT t FROM GateInToken t WHERE t.tokenId = :tokenId"),
        @NamedQuery(name = "GateInToken.findByUser", query = "SELECT t FROM GateInToken t WHERE t.username = :username"),
        @NamedQuery(name = "GateInToken.findExpired", query = "SELECT t FROM GateInToken t WHERE t.expirationTime < :expireTime"),
        @NamedQuery(name = "GateInToken.cleanTokens", query = "DELETE FROM GateInToken t WHERE t.expirationTime < :expireTime")
})
public class TokenEntity implements Serializable {
    private static final long serialVersionUID = 6633792468705838255L;

    @Id
    @SequenceGenerator(name="SEQ_GATEIN_TOKEN_ID_GENERATOR", sequenceName="SEQ_GATEIN_TOKEN_ID_GENERATOR")
    @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_GATEIN_TOKEN_ID_GENERATOR")
    @Column(name = "ID")
    private Long            id;

    @Column(name = "TOKEN_ID")
    private String tokenId;

    @Column(name = "TOKEN_HASH")
    private String tokenHash;

    @Column(name = "USERNAME")
    private String            username;

    @Column(name = "PASSWORD")
    private String            password;

    @Column(name="EXPIRATION_TIME", nullable = false)
    private Long expirationTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getExpirationTime() {
        return expirationTime != null && expirationTime > 0 ? new Date(expirationTime) : null;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = (expirationTime != null ? expirationTime.getTime() : -1);
    }
}
