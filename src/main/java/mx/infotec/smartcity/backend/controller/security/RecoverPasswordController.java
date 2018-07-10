package mx.infotec.smartcity.backend.controller.security;

import mx.infotec.smartcity.backend.model.TokenRequest;
import mx.infotec.smartcity.backend.service.exception.ServiceException;
import mx.infotec.smartcity.backend.service.recovery.TokenRecoveryService;
import mx.infotec.smartcity.backend.utils.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service used for recovery password operations.
 * 
 * @author Infotec
 */
@RestController
public class RecoverPasswordController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecoverPasswordController.class);

  @Autowired
  private TokenRecoveryService        recoveryService;
  
  @Value("${idm.admin.username}")
  private String                      idmUser;

    /**
     * Method used to start password recovery.
     *
     * @param tokenRequest Object with user's email
     * @return Response
     */
    @RequestMapping(method = RequestMethod.POST, value = "/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody TokenRequest tokenRequest) {
        if (tokenRequest.getUsername().equals(idmUser)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Can't change password");
        } else {
            try {
                recoveryService.recoveryPassword(tokenRequest.getUsername());
            } catch (ServiceException e) {
                LOGGER.error("forgotPassword error, cause: ", e);
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("success");
        }
    }

  /**
   * Verifies if email's recovery token is correct
   * 
   * @param recoveryToken
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/valid-token")
  public ResponseEntity<?> validToken(
      @RequestHeader(value = Constants.RECOVERY_TOKEN) String recoveryToken) {
    try {
      if (recoveryService.validateTokenRecovery(recoveryToken)) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("success");
      } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
      }
    } catch (ServiceException e) {
      LOGGER.error("validToken error, cause: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getCause());
    }
  }

  /**
   * Restores the given password using the verification token
   * 
   * @param recoveryToken
   * @param tokenRequest
   * @return
   */
  @RequestMapping(method = RequestMethod.POST, value = "/restore-password",
      consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> restorePassword(
      @RequestHeader(value = Constants.RECOVERY_TOKEN) String recoveryToken,
      @RequestBody TokenRequest tokenRequest) {
    try {
      if (recoveryService.updatePassword(recoveryToken, tokenRequest)) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("success");
      } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
      }
    } catch (ServiceException e) {
      LOGGER.error("validToken error, cause: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getCause());
    }
  }

}
