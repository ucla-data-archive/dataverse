package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.userdata.OffsetPageValues;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {
  
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    UserServiceBean userService;
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;
    private Integer selectedPage = 1;
    private UserListMaker userListMaker = null;

    private Pager pager;
    private List<AuthenticatedUser> userList;
    
    private String searchTerm;

    public String init() {
        System.out.println("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            userListMaker = new UserListMaker(userService);
            runUserSearch();
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        return null;
    }
    
    public boolean runUserSearchWithPage(Integer pageNumber){
        System.err.println("runUserSearchWithPage");
        setSelectedPage(pageNumber);
        runUserSearch();
        return true;
    }
    
    public boolean runUserSearch(){

        msgt("Run the search!");


        /**
         * (1) Determine the number of users returned by the count        
         */
        Long userCount = userListMaker.getUserCountWithSearch(searchTerm);
        
        msgt("userCount: " + userCount);
        int itemsPerPage = UserListMaker.ITEMS_PER_PAGE;
        /**
         * (2) Based on the page number and number of users,
         *      determine the off set to use when querying
         */
        OffsetPageValues offsetPageValues = userListMaker.getOffset(userCount, getSelectedPage(), itemsPerPage);
        
        /**
         * Update the pageNumber on the page
         */
        setSelectedPage(offsetPageValues.getPageNumber());        
        int offset = offsetPageValues.getOffset();
        
        msg("offset: " + offset);

        /**
         * (3) Run the search and update the user list 
         */
        String sortKey = null;
        this.userList = userService.getAuthenticatedUserList(searchTerm, sortKey, UserListMaker.ITEMS_PER_PAGE, offsetPageValues.getOffset());

        msg("userList size: " + userList.size());

        makeNewPager(userCount.intValue(), itemsPerPage, getSelectedPage());

        msg("pager: " + pager.asJSONString());

        return true;
    }

    /** 
     * Make sure there is a pager--even if the user count is 0
     * 
     * @param userCount
     * @param displayItemsPerPage
     * @param chosenPage
     * @return 
     */
    private boolean makeNewPager(Integer userCount, Integer displayItemsPerPage, Integer chosenPage){
        
        if (userCount == null){
            userCount = 0;
        }
        if (chosenPage == null){
            chosenPage = 1;
        }
        if (displayItemsPerPage == null){
            displayItemsPerPage = UserListMaker.ITEMS_PER_PAGE;
        }
        
        pager = new Pager(userCount, displayItemsPerPage, chosenPage);
        
        return true;
    }

    
    public String getListUsersAPIPath() {
        //return "ok";
        return Admin.listUsersFullAPIPath;
    }

    /** 
     * Number of total users
     * @return 
     */
    public String getUserCount() {
        //return userService.getTotalUserCount();

        return NumberFormat.getNumberInstance(Locale.US).format(userService.getTotalUserCount());
    }

    /** 
     * Number of total Superusers
     * @return 
     */
    public Long getSuperUserCount() {
        
        return userService.getSuperUserCount();
    }

    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }

    /**
     * Pager for when user list exceeds the number of display rows
     * (default: UserListMaker.ITEMS_PER_PAGE)
     * 
     * @return 
     */
    public Pager getPager() {
        return this.pager;
    }

    public void setSelectedPage(Integer pgNum){
        if ((pgNum == null)||(pgNum < 1)){
            this.selectedPage = 1;
        }
        selectedPage = pgNum;
    }

    public Integer getSelectedPage(){
        if ((selectedPage == null)||(selectedPage < 1)){
            setSelectedPage(null);            
        }
        return selectedPage;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    public void toggleSuperUserStatus(AuthenticatedUser user){
        logger.info("Toggling user's "+user.getIdentifier()+" superuser status;");
        user.setSuperuser(!user.isSuperuser());
        logger.info("Attempting to save user "+user.getIdentifier());
        authenticationService.update(user);
        
    }
    
    /* Methods for an alternative, two-step implementation: first a confirmation popup, 
       "are you sure you want to toggle the superuser status of user ...?"
       - and if yes, save... 
       
    AuthenticatedUser selectedUser = null; 
    
    public void setUserToToggleSuperuserStatus(AuthenticatedUser user) {
        logger.info("selecting user "+user.getIdentifier());
        selectedUser = user; 
    }
    */
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
}
