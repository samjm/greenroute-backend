package mx.infotec.smartcity.backend.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import mx.infotec.smartcity.backend.model.transport.ProfilePublicTransport;
import mx.infotec.smartcity.backend.model.transport.PublicTransport;
import mx.infotec.smartcity.backend.model.transport.TransportSchedule;
import mx.infotec.smartcity.backend.model.transport.WeekDay;
import mx.infotec.smartcity.backend.model.DayName;
import mx.infotec.smartcity.backend.model.Time;

import mx.infotec.smartcity.backend.persistence.ProfilePublicTransportRepository;
import mx.infotec.smartcity.backend.persistence.PublicTransportRepository;
import mx.infotec.smartcity.backend.persistence.TransportScheduleRepository;
import mx.infotec.smartcity.backend.utils.Constants;
import mx.infotec.smartcity.backend.utils.ControllerUtils;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * RestService Public Transport.
 *
 * @author Benjamin Vander Stichelen
 */
@RestController
@RequestMapping("/public-transport")
public class PublicTransportController {

  private static final Logger              LOGGER    =
      LoggerFactory.getLogger(PublicTransportController.class);

  @Autowired
  private PublicTransportRepository        publicTransportRepository;
  @Autowired
  private TransportScheduleRepository transportScheduleRepository;


  @Autowired
  private ProfilePublicTransportRepository profilePublicTransportRepository;

  private int                              SIZE      = 5;
  private String                           PROFILEID = "ProfileId";

  /**
   * Returns a list of all public transports
   * 
   * @return
   */
  @RequestMapping(method = RequestMethod.GET)
  public List<PublicTransport> getByAll() {
    return publicTransportRepository.findAll();
  }
  
  /**
   * Returns a public transport for a given id
   * 
   * @param id
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  public PublicTransport getById(@PathVariable("id") String id) {
    return publicTransportRepository.findOne(id);
  }
  
  /**
   * Search for a public transport based on the request parameters
   * 
   * @param name
   * @param routeName
   * @param weekdays
   * @param departureTime
   * @param arrivalTime
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/search")
  public List<PublicTransport> search(
		  @RequestParam(value = "name", required = false) String name,
          @RequestParam(value = "routename", required = false) String routeName,
          @RequestParam(value = "weekday", required = false) List<String> weekdays,
          @RequestParam(value = "departuretime", required = false) String departureTime,
          @RequestParam(value = "arrivaltime", required = false) String arrivalTime) {
   
    if(name == null && routeName == null && weekdays == null && departureTime == null && arrivalTime == null){
    	return publicTransportRepository.findAll();
    }
    Time depTime = null;
    if(departureTime != null){
         depTime = stringToTime(departureTime);
    }
    Time arrTime = null;
    if(arrivalTime != null){
    	arrTime = stringToTime(arrivalTime);
    }
    if(name != null ){
    	List<TransportSchedule> transportSchedules = validateAndSearchTransportSchedules(routeName, weekdays, depTime, arrTime);
        if(transportSchedules != null){
            return publicTransportRepository.findByNameLikeAndTransportSchedulesIn(name,transportSchedules);
        } else
        {
            return publicTransportRepository.findByNameLike(name);
        }
        
    } else {
        List<TransportSchedule> transportSchedules = validateAndSearchTransportSchedules(routeName, weekdays, depTime, arrTime);
        if(transportSchedules != null){
            return publicTransportRepository.findByTransportSchedulesIn(transportSchedules);
        }
    }
    
    
   
    return null;
  }
  
  /**
   * Returns a list of paginated public transport
   * 
   * @param page
   * @param size
   * @param name
   * @param routeName
   * @param weekdays
   * @param departureTime
   * @param arrivalTime
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/page/{page}/{size}")
  public Page<?> getByPageSize(
		  @PathVariable("page") String page,
		  @PathVariable("size") String size,
		  @RequestParam(value = "name", required = false) String name,
          @RequestParam(value = "routename", required = false) String routeName,
          @RequestParam(value = "weekday", required = false) List<String> weekdays,
          @RequestParam(value = "departuretime", required = false) String departureTime,
          @RequestParam(value = "arrivaltime", required = false) String arrivalTime) {
    Pageable pageable = new PageRequest(Integer.parseInt(page), Integer.parseInt(size));
   
    if(name == null && routeName == null && weekdays == null && departureTime == null && arrivalTime == null){
    	return publicTransportRepository.findAll(pageable);
    }
    Time depTime = null;
    if(departureTime != null){
         depTime = stringToTime(departureTime);
    }
    Time arrTime = null;
    if(arrivalTime != null){
    	arrTime = stringToTime(arrivalTime);
    }
    if(name != null ){
    	List<TransportSchedule> transportSchedules = validateAndSearchTransportSchedules(routeName, weekdays, depTime, arrTime);
        if(transportSchedules != null){
            return publicTransportRepository.findByNameLikeAndTransportSchedulesIn(name,transportSchedules, pageable);
        }
        
    } else {
        List<TransportSchedule> transportSchedules = validateAndSearchTransportSchedules(routeName, weekdays, depTime, arrTime);
        if(transportSchedules != null){
            return publicTransportRepository.findByTransportSchedulesIn(transportSchedules, pageable);
        }
    }
    
    
   
    return null;
  }

  /**
   * Returns a list of paginated public transport with default page size (5)
   * 
   * @param page
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/page/{page}")
  public Page<PublicTransport> getByPage(@PathVariable("page") int page) {
    Pageable pageable = new PageRequest(page, SIZE);
    return publicTransportRepository.findAll(pageable);
  }

  /**
   * Returns public transport by profile id
   * 
   * @param id
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/profile/{id}")
  public ResponseEntity<?> getPublicTransportWithProfileId(@PathVariable("id") String id) {
    try {
      return ResponseEntity.accepted()
          .body(this.publicTransportRepository.findAll(getListaIdPublicTransportIterator(id)));
      // return ResponseEntity.accepted().body(getListaPublicTransport(id));

    } catch (Exception ex) {
      LOGGER.error("Error at update", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }
  }


  /**
   * Returns public transport by profile id paginated with default page size(5)
   * 
   * @param id
   * @param page
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/profile/{id}/page/{page}")
  public ResponseEntity<?> getPublicTransportWithProfileId(@PathVariable("id") String id,
      @PathVariable("page") int page) {
    try {
      Pageable pageable = new PageRequest(page, SIZE);

      Iterable<PublicTransport> ipt =
          this.publicTransportRepository.findAll(getListaIdPublicTransportIterator(id));
      List<PublicTransport> lpt = new ArrayList<PublicTransport>();
      ipt.forEach(lpt::add);
      return ResponseEntity.accepted()
          .body(new PageImpl<PublicTransport>(lpt, pageable, lpt.size()));
    } catch (Exception ex) {
      LOGGER.error("Error at update", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }
  }

  /**
   * Returns public transport by profile id paginated
   * 
   * @param id
   * @param page
   * @param size
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, value = "/profile/{id}/page/{page}/size/{size}")
  public ResponseEntity<?> getPublicTransportWithProfileId(@PathVariable("id") String id,
      @PathVariable("page") int page, @PathVariable("size") int size) {
    try {
      Pageable pageable = new PageRequest(page, size);

      Iterable<PublicTransport> ipt =
          this.publicTransportRepository.findAll(getListaIdPublicTransportIterator(id));
      List<PublicTransport> lpt = new ArrayList<PublicTransport>();
      ipt.forEach(lpt::add);
      return ResponseEntity.accepted()
          .body(new PageImpl<PublicTransport>(lpt, pageable, lpt.size()));
    } catch (Exception ex) {
      LOGGER.error("Error at update", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }
  }

  private Iterable<String> getListaIdPublicTransportIterator(String idProfile) {
    Iterable<String> iterable =
        getListaIdPublicTransport(this.profilePublicTransportRepository.findByIdProfile(idProfile));


    return iterable;

  }

  private List<String> getListaIdPublicTransport(List<ProfilePublicTransport> lppt) {
    List<String> idsPublicTransport = new ArrayList<String>();
    for (ProfilePublicTransport ppt : lppt) {
      idsPublicTransport.add(ppt.getIdPublicTransport());
    }
    return idsPublicTransport;
  }

  private List<PublicTransport> getListaPublicTransport(String id) {
    List<PublicTransport> lPublicTransport = new ArrayList<PublicTransport>();
    for (String idTrasportePublico : getListaIdPublicTransport(
        this.profilePublicTransportRepository.findByIdProfile(id))) {
      addingToPublicTransportList(lPublicTransport, idTrasportePublico);
    }
    return lPublicTransport;
  }


  private void addingToPublicTransportList(List<PublicTransport> lPublicTransport,
      String idTrasportePublico) {
    PublicTransport pt = this.publicTransportRepository.findById(idTrasportePublico);
    if (pt != null) {
      lPublicTransport.add(pt);
    }
  }

  private List<PublicTransport> getListaPublicTransport(String id, Pageable pageable) {
    List<PublicTransport> lPublicTransport = new ArrayList<PublicTransport>();
    for (String idTrasportePublico : getListaIdPublicTransport(
        this.profilePublicTransportRepository.findByIdProfile(id, pageable))) {
      addingToPublicTransportList(lPublicTransport, idTrasportePublico);
    }

    return lPublicTransport;
  }

  /**
   * Removes the specified public transport by id
   * 
   * @param id
   * @param removeReason
   * @param request
   * @return
   */
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  public ResponseEntity<?> deleteByID(@PathVariable String id,
          @RequestParam(value = "removeReason", required = true) String removeReason,
          HttpServletRequest request) {
    try {
      PublicTransport publicTransport = publicTransportRepository.findById(id);
      
      if (publicTransport != null && ControllerUtils.canDeleteUpdate(publicTransport.getCreatorId(), request)) {
          ProfilePublicTransport profilePublicTransport =
              profilePublicTransportRepository.findByIdPublicTransport(id);
          publicTransportRepository.delete(id);
          profilePublicTransportRepository.delete(profilePublicTransport);
          
          // TODO: Persists
          LOGGER.info("Public transport deleted, reason: {}", removeReason);
          
          return ResponseEntity.accepted().body("deleted");          
      } else {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only SA user or owner data can delete");
      }
    } catch (Exception ex) {
      LOGGER.error("Error at delete", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }
  }

  /**
   * Creates a new public transport object
   * 
   * @param publicTransport
   * @param request
   * @return
   */
  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> add(@RequestBody PublicTransport publicTransport, HttpServletRequest request) {
    if (publicTransport.getId() != null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID must be null");
    } else if (!isValid(publicTransport)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name, BrandName and ModelName are required");
    } else {
      try {
        publicTransport.setCreatorId(ControllerUtils.getUserId(request));
        publicTransport.setDateCreated(new Date());
        
        PublicTransport publicTransportRepro = publicTransportRepository.insert(publicTransport);
        this.profilePublicTransportRepository
            .insert(new ProfilePublicTransport(this.PROFILEID, publicTransport.getId()));
        return ResponseEntity.accepted().body(publicTransportRepro);
      } catch (Exception ex) {
        LOGGER.error("Error at insert", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
      }
    }
  }

  /**
   * Updates a public transport
   * 
   * @param publicTransport
   * @param id
   * @param request
   * @return
   */
  @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
      value = "/{id}")
  public ResponseEntity<?> update(@RequestBody PublicTransport publicTransport,
      @PathVariable("id") String id, HttpServletRequest request) {
    try {
      PublicTransport publicTransportOrin = publicTransportRepository.findById(id);
      
      if (publicTransportOrin == null) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID don't exists");
      } else if (!isValid(publicTransport)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Name, BrandName and ModelName are required");
      } else if (!ControllerUtils.canDeleteUpdate(publicTransportOrin.getCreatorId(), request)) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only SA user or owner data can update");
      } else {
        if (publicTransport.getId() != null) {
          LOGGER.warn("ID from object is ignored");
        }

        publicTransport.setId(id);
        publicTransport.setDateCreated(publicTransportOrin.getDateCreated());
        publicTransport.setDateModified(new Date());
        publicTransport.setCreatorId(publicTransportOrin.getCreatorId());
        
        publicTransportRepository.save(publicTransport);

        return ResponseEntity.accepted().body("updated");
      }
    } catch (Exception ex) {
      LOGGER.error("Error at update", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
    }
  }

  private boolean isValid(PublicTransport publicTransport) {
    return publicTransport != null && publicTransport.getName() != null && publicTransport.getBrandName() != null && publicTransport.getModelName() != null && publicTransport.getPublicTransportFuelType() != null && publicTransport.getPublicTransportFuelType().getId() != null;
  }
  // needs to have format HH:mm
  private Time stringToTime(String str){
	  SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm"); //HH = 24h format
	  dateFormat.setLenient(false); //this will not enable 25:67 for example
	  Time time = new Time();
	  Calendar calendar = Calendar.getInstance();
	  
	  try {

		Date date = dateFormat.parse(str);
		calendar.setTime(date);
		time.setHour(calendar.get(Calendar.HOUR_OF_DAY));
		time.setMinute(calendar.get(Calendar.MINUTE));
		return time;
	} catch (ParseException e) {
		throw new RuntimeException("Invalid time "+ str, e);
	}
	  
  }

  private List<WeekDay> createWeekdays(){
	  List<WeekDay> weekdays = new ArrayList<WeekDay>();
	  for(DayName dayName: DayName.values()){
		  WeekDay weekDay = new WeekDay();
		  weekDay.setActive(false);
		  weekDay.setDayName(dayName);
                  weekdays.add(weekDay);
	  }
	  return weekdays;
  }
  
  private void activateWeekDay(List<WeekDay> weekDays,String dayname){
	  for(WeekDay weekDay: weekDays){
		  if(weekDay.getDayName().equals(DayName.valueOf(dayname))){
			  weekDay.setActive(true);
		  }
	  }
  }

    private List<TransportSchedule> validateAndSearchTransportSchedules(String routeName, List<String> weekdays, Time departureTime, Time arrivalTime) {
        if( routeName != null){
            if( weekdays != null){
                if(departureTime != null){
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByActiveDaysAndDepartureTimeAndArrivalTimeRouteNameQuery( weekdays, departureTime, arrivalTime,routeName);
                    } else {
                        return this.transportScheduleRepository.findByActiveDaysAndDepartureTimeRouteNameQuery( weekdays, departureTime, routeName);
                    }
                } else {
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByActiveDaysArrivalTimeRouteNameQuery( weekdays, arrivalTime,routeName);
                    } else {
                        return this.transportScheduleRepository.findByActiveDaysAndRouteNameQuery( weekdays, routeName);
                    }
                }
            } else {
                if(departureTime != null){
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByDepartureTimeAndArrivalTimeRouteNameQuery(departureTime, arrivalTime,routeName);
                    } else {
                        return this.transportScheduleRepository.findByDepartureTimeAndRouteNameQuery(departureTime, routeName);
                    }
                } else {
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByArrivalTimeAndRouteNameQuery(arrivalTime,routeName);
                    } else {
                        return this.transportScheduleRepository.findByRouteName( routeName);
                    }
                }
            } 
        } else {
            if( weekdays != null){
                if(departureTime != null){
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByActiveDaysAndDepartureTimeAndArrivalTimeQuery( weekdays, departureTime, arrivalTime);
                    } else {
                        return this.transportScheduleRepository.findByActiveDaysAndDepartureTimeQuery( weekdays, departureTime);
                    }
                } else {
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByActiveDaysArrivalTimeQuery( weekdays, arrivalTime);
                    } else {
                        return this.transportScheduleRepository.findByActiveDaysQuery( weekdays);
                    }
                }
            } else {
                if(departureTime != null){
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByWeekDaysDepartureTimeAndWeekDaysArrivalTime(departureTime, arrivalTime);
                    } else {
                        return this.transportScheduleRepository.findByWeekDaysDepartureTime(departureTime);
                    }
                } else {
                    if(arrivalTime != null){
                        return this.transportScheduleRepository.findByWeekDaysArrivalTime(arrivalTime);
                    } else {
                        return null;
                    }
                }
            }
                    
        }
        
            
        
        
    }
  
}
