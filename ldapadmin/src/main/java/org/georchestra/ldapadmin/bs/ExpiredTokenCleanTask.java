package org.georchestra.ldapadmin.bs;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.georchestra.ldapadmin.model.UserToken;
import org.georchestra.ldapadmin.dao.UserTokenDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This task searches and removes the expired tokens generated when for the "lost password" use case.
 *
 * @author Mauricio Pazos
 *
 */
public class ExpiredTokenCleanTask implements Runnable {

	private static final Log LOG = LogFactory.getLog(ExpiredTokenCleanTask.class.getName());

	@Autowired
	private UserTokenDao userTokenRepo;

	private long delayInMilliseconds;

	public void setDelayInMilliseconds(long delayInMiliseconds) {
		this.delayInMilliseconds = delayInMiliseconds;
	}

	/**
	 * Removes the expired tokens
	 *
	 * This task is scheduled taking into account the delay period.
	 */
	@Override
	public void run() {

		Calendar calendar = Calendar.getInstance();

		long now = calendar.getTimeInMillis();
		Date expired = new Date(now - this.delayInMilliseconds);

		for(UserToken userToken : userTokenRepo.findByBeforeCreationDate(expired))
			userTokenRepo.delete(userToken);

	}
}
