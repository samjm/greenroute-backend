package mx.infotec.smartcity.backend.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import mx.infotec.smartcity.backend.model.IdentityUser;
import mx.infotec.smartcity.backend.model.Role;
import mx.infotec.smartcity.backend.model.UserModel;
import mx.infotec.smartcity.backend.service.UserService;
import mx.infotec.smartcity.backend.service.exception.ServiceException;
import mx.infotec.smartcity.backend.utils.Constants;

/**
 * Performs administrative tasks (users's operations)
 *
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService keystoneUserService;


    /**
     * Creates a user and stores its user profile
     * 
     * @param model
     * @param request
     * @return
     */
    @RequestMapping(value = "/user/register", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> userRegistration(@RequestBody UserModel model, HttpServletRequest request) {
        
        try {
            
            if (!keystoneUserService.isRegisteredUser(model.getEmail())) {

                if (keystoneUserService.createUserByAdmin(model)) {
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body("Success");
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
            }
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Registered User");
        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Deletes user and removes its profile
     * 
     * @param model
     * @return
     */
    @RequestMapping(value = "/user/delete", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> deleteUser(@RequestBody UserModel model) {
        try {
            if (keystoneUserService.isRegisteredUser(model.getEmail())) {

                if (keystoneUserService.deleteUserByAdmin(model)) {
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body("Success");
                }

            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User Not Fond");
        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Returns the list of UserProfiles registered with its roles
     * 
     * @return
     */
    @RequestMapping(value = "/user/list", method = RequestMethod.GET)
    public ResponseEntity<?> userList() {
        try {
            List<UserModel> userModelList = keystoneUserService.getUserModelList();
            if (userModelList != null && !userModelList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(userModelList);
            }

            return ResponseEntity.status(HttpStatus.CONFLICT).body("Users Not Found");
        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Search users based on UserProfile parameters
     * 
     * @param model
     * @return
     */
    @RequestMapping(value = "/user/filter", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> userTry(@RequestBody UserModel model) {
        try {
            List<UserModel> models = keystoneUserService.filterUsers(model);
            if (models != null && !models.isEmpty()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(models);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Users Not Found");
        } catch (ServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
