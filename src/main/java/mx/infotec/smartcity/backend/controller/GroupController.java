package mx.infotec.smartcity.backend.controller;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import mx.infotec.smartcity.backend.model.Group;
import mx.infotec.smartcity.backend.persistence.GroupRepository;
import mx.infotec.smartcity.backend.persistence.UserProfileRepository;

/**
 * RestService Public Transport.
 *
 * @author Benjamin Vander Stichelen
 */
@RestController
@RequestMapping("/groups")
public class GroupController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private GroupRepository groupRepository;
    
    @Autowired
    private UserProfileRepository userProfileRepository;

    private int SIZE = 5;
    
    /**
     * Returns a list of all groups created
     * 
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public List<Group> getByAll() {
        return groupRepository.findAll();
    }
    
    /**
     * Returns a group for a given id
     * 
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{id}")
    public Group getById(@PathVariable("id") Integer id) {
        Group grupo = groupRepository.findOne(id);
        return grupo;
    }

    /**
     * Returns a list of paginated groups
     * 
     * @param page
     * @param size
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/page/{page}/{size}")
    public Page<Group> getByPageSize(@PathVariable("page") String page, @PathVariable("size") String size) {
        Pageable pageable = new PageRequest(Integer.parseInt(page), Integer.parseInt(size));

        return groupRepository.findAll(pageable);
    }

    /**
     * Returns a list of paginated groups using a default page size(5)
     * 
     * @param page
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, value = "/page/{page}")
    public Page<Group> getByPage(@PathVariable("page") int page) {
        Pageable pageable = new PageRequest(page, SIZE);
        return groupRepository.findAll(pageable);
    }

    /**
     * Deletes a group specified by its id 
     * 
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    public ResponseEntity<?> deleteByID(@PathVariable Integer id) {
        try {
            
            
            if (userProfileRepository.findByGroupID(id).size() > 0)
            {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"You can not remove this group, it is related to one or more users \"}");
            }
            else
            {
                groupRepository.delete(id);
                return ResponseEntity.accepted().body("deleted");
            }
        } catch (Exception ex) {
            LOGGER.error("Error at delete", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"" + ex.getMessage() + "\"}");
        }
    }

    /**
     * Creates a new group with a list of alert types 
     * 
     * @param group
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> add(@Valid @RequestBody Group group) {
        if (group.getId() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID must be null");
        } else {
            try {
                
                if (groupRepository.findByGroup(group.getGroup()) != null)
                {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"The group you want to add already exists\"}");
                }
                
                // Add this code lines to set numeric id to vehicle
                // type////////////////////////////////////////
                Group max = groupRepository.findFirstByOrderByIdDesc();
                if (max != null) {
                    group.setId(max.getId() + 1);
                } else {
                    group.setId(1);
                }
                //////////////////////////////////////////////////////////////////////////////////////////////
                
                group.setDateCreated(new Date());
                group.setDateModified(new Date());
                Group GroupRepro = groupRepository.insert(group);
                return ResponseEntity.created(new URI("")).body(group);
            } catch (Exception ex) {
                LOGGER.error("Error at insert", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
            }
        }
    }

    /**
     * Updates the specified group
     * 
     * @param group
     * @param id
     * @return
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, value = "/{id}")
    public ResponseEntity<?> update(@RequestBody Group group, @PathVariable("id") Integer id) {
        try {
            if (groupRepository.exists(id)) {
                
                Group groupSameName = groupRepository.findByGroup(group.getGroup());
                if (groupSameName != null && !Objects.equals(groupSameName.getId(), id))
                {
                    return ResponseEntity.badRequest().body("{\"error\":\"There is already a group called '" + group.getGroup() + "'\"}");
                }
                
                if (group.getId() != null) {
                    LOGGER.warn("ID from object is ignored");
                }
                group.setDateModified(new Date());
                group.setId(id);
                groupRepository.save(group);

                return ResponseEntity.accepted().body(group);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID don't exists");
            }
        } catch (Exception ex) {
            LOGGER.error("Error at update", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

}
