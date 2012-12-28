// (C) Copyright 2003 by Enrico Liboni (enrico(at)computer.org)
// This piece of code is distributed under the GNU Public Licence
// http://www.gnu.org/licenses/gpl.txt
//
//
// Usage in other classes:
//     ValidateNTLogin vnl = new ValidateNTLogin();
//     vnl.setDomainController(domainCtrlAddr);
//     if (vnl.isValidNTLogin(domain,user,pass)) {
//	System.err.println("Authentication Succesful");
//     } else {
//	System.err.println("Authentication Error");
//     }
//
package art.utils;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to enable NTLM authentication
 *
 * @author Enrico Liboni
 */
public class ValidateNTLogin {

	final static Logger logger = LoggerFactory.getLogger(ValidateNTLogin.class);
	UniAddress domainController;

	/**
	 *
	 */
	public ValidateNTLogin() {
	}

	/**
	 *
	 * @param ntdomain
	 * @param user
	 * @param pass
	 * @return <code>true</code> if successfully authenticated
	 */
	public synchronized boolean isValidNTLogin(String ntdomain, String user, String pass) {

		NtlmPasswordAuthentication mycreds = new NtlmPasswordAuthentication(ntdomain, user, pass);
		try {
			//jcifs.smb.Session.logon()
			SmbSession.logon(domainController, mycreds);
			// SUCCESS
			return true;
		} catch (SmbAuthException sae) {
			// AUTHENTICATION FAILURE
			logger.error("Authentication Failure", sae);
			return false;
		} catch (SmbException se) {
			// NETWORK PROBLEMS?
			logger.error("Error", se);
			return false;
		} catch (Exception e) {
			logger.error("Error", e);
			return false;
		}
	}

	/**
	 *
	 * @param domainCtrl
	 * @return <code>true</code> if successful
	 */
	public boolean setDomainController(String domainCtrl) {
		try {
			domainController = UniAddress.getByName(domainCtrl);
			return true;
		} catch (Exception e) {
			logger.error("Error", e);
			return false;
		}

	}
}
