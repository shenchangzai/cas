package org.apereo.cas.adaptors.gauth;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.adaptors.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.authentication.OneTimeToken;
import org.apereo.cas.otp.repository.token.BaseOneTimeTokenRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;

/**
 * This is {@link GoogleAuthenticatorJpaTokenRepository}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@EnableTransactionManagement(proxyTargetClass = true)
@Transactional(transactionManager = "transactionManagerGoogleAuthenticator")
@Slf4j
public class GoogleAuthenticatorJpaTokenRepository extends BaseOneTimeTokenRepository {


    @PersistenceContext(unitName = "googleAuthenticatorEntityManagerFactory")
    private transient EntityManager entityManager;

    private final long expireTokensInSeconds;

    public GoogleAuthenticatorJpaTokenRepository(final long expireTokensInSeconds) {
        this.expireTokensInSeconds = expireTokensInSeconds;
    }

    @Override
    public void cleanInternal() {
        final int count = this.entityManager.createQuery("DELETE FROM " + GoogleAuthenticatorToken.class.getSimpleName()
            + " r where r.issuedDateTime>= :expired")
            .setParameter("expired", LocalDateTime.now().minusSeconds(this.expireTokensInSeconds))
            .executeUpdate();
        LOGGER.debug("Deleted [{}] expired previously used token record(s)", count);
    }

    @Override
    public void store(final OneTimeToken token) {
        this.entityManager.merge(token);
    }

    @Override
    public GoogleAuthenticatorToken get(final String uid, final Integer otp) {
        try {
            final GoogleAuthenticatorToken r =
                this.entityManager.createQuery("SELECT r FROM " + GoogleAuthenticatorToken.class.getSimpleName()
                    + " r where r.userId = :userId and r.token = :token", GoogleAuthenticatorToken.class)
                    .setParameter("userId", uid)
                    .setParameter("token", otp)
                    .getSingleResult();
            return r;
        } catch (final NoResultException e) {
            LOGGER.debug("No record could be found for google authenticator id [{}]", uid);
        }
        return null;
    }
}
