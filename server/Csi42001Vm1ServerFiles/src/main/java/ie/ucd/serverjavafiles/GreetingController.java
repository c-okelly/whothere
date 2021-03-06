package ie.ucd.serverjavafiles;

import java.sql.SQLException;
import javax.mail.MessagingException;
import java.sql.ResultSet;
import javax.sql.DataSource;
import java.sql.Connection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;


import java.io.BufferedInputStream;
import org.springframework.util.FileCopyUtils;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import org.apache.tomcat.util.http.fileupload.IOUtils;

@Controller
public class GreetingController {
        
    @Autowired
    DataSource dataSource;
        
//  @RequestMapping(value="/index", method=RequestMethod.GET)
//  public String indexPage(Model model) {
//	return "main";
//  }
	
//  In order of navigation. Upload page mapped within contorller
	
    @RequestMapping(value={"/", "/main"}, method=RequestMethod.GET)
    public String indexPageFromLocalhost(Model model) {
	return "main";
    }
        
    @RequestMapping(value="/admincontrol", method=RequestMethod.GET)
    public String adminControlPage(Model model) throws SQLException {
        model.addAttribute("upgradeModel", new Upgrade());
        return "admincontrol";
    }
    
    @RequestMapping(value="/admincontrol", method=RequestMethod.POST)
    public String adminControlPage(@ModelAttribute Upgrade upgrade, Model model) throws SQLException {
        model.addAttribute("upgradeModel", new Upgrade());
        SqlQueries query = new SqlQueries(dataSource.getConnection());
        boolean check = query.sqlUpgradeUsers(upgrade);
        query.closeConnections();
        if (check) {
            return "redirect: /admincontrol?success";
        }
        return "redirect: /admincontrol?failure";
    }
	
    @RequestMapping(value="/error", method=RequestMethod.GET)
	public String errorPage(Model model) {
	    return "error";
    }
          
    @RequestMapping(value="/api_docs", method=RequestMethod.GET)
    public String apiDocs(Model model) {
	return "api_docs";
    }
	
    @RequestMapping(value="/pdf_reports", method=RequestMethod.GET)
    public String pdfReports(Model model) {
	model.addAttribute("searchModel", new Search());
	return "pdf_reports";
    }
	
    @RequestMapping(value="/pdf_reports", method=RequestMethod.POST)
    public void pdfReportsPost(@ModelAttribute Search search, HttpServletResponse response, Model model) throws IOException {
        model.addAttribute("searchModel", new Search());
        // Code goes here to call R Script to generate PDF report, and the return value of name/location
        String directory = "src/main/resources_scripts/dataAnalysis/";
        String params = search.getSearchMethod() + " " + search.getSearchTerms() + " ";
        params = params + directory;
        String fileName = helpers.activateScript("Rscript", "dataAnalysis", "create_pdf.R", params);
        File file = new File(fileName);
        response.setContentType("application/pdf");
        response.setHeader("Content-disposition", "attachment; filename=" + search.getSearchMethod() + search.getSearchTerms());
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        OutputStream outputStream = response.getOutputStream();
        IOUtils.copy(inputStream, outputStream);
        IOUtils.closeQuietly(inputStream);
        response.flushBuffer();
        file.delete();
    }
		
    @RequestMapping(value="/registration", method=RequestMethod.GET)
    public String registrationPage(Model model){
        model.addAttribute("registerModel", new Registration());
        return "registration";
    }
        
    @RequestMapping(value="/registration", method=RequestMethod.POST)
    public String registrationPost(@ModelAttribute Registration register, Model model) throws SQLException {
        model.addAttribute("registerModel", new Registration());
        SqlQueries query = new SqlQueries(dataSource.getConnection());
        if (register.getRegistrationCode().equals("TEST")){
            if (query.sqlCheckUserExists(register.getUserName())){
                query.sqlSetUsers(register);
                query.closeConnections();
                return "redirect: /login?newaccount";
            }
	    return "redirect: /registration?errorN";
        }
        query.closeConnections();
        return "redirect: /registration?errorR";
    }
    
    @RequestMapping(value="/site_map", method=RequestMethod.GET)
    public String siteMap(Model model) {
	return "site_map";
    }
	
    @RequestMapping(value="/contact", method=RequestMethod.GET)
    public String contactPage(Model model) throws MessagingException {
        model.addAttribute("contactModel", new Email());
        return "contact";
    }
        
    @RequestMapping(value="/contact", method=RequestMethod.POST)
    public String contactPage(@ModelAttribute Email email, Model model) throws MessagingException {
        model.addAttribute("contactModel", new Email());
        SendMail mail = new SendMail();
        email.addEmailInMsg(email.getMsg());
        try {
	    mail.mailSender(email.getName(), email.getEmail(), email.getMsg());
	    return "redirect: /contact?success";
	} catch (MessagingException exc) {
            return "redirect: /contact?failure";
        }
    }
	
// Internal page elements    
    
    @RequestMapping(value="/header", method=RequestMethod.GET)
    public String headerPage(Model model) {
	return "header";
    }

    @RequestMapping(value="/nav_bar", method=RequestMethod.GET)
    public String navPage(Model model) {
        return "nav_bar";
    }
	
    @RequestMapping(value="/admin_nav_bar", method=RequestMethod.GET)
    public String adminNavPage(Model model) {
        return "admin_nav_bar";
    }

    @RequestMapping(value="/footer", method=RequestMethod.GET)
    public String footerPage(Model model) {
        return "footer";
    }
    
    @RequestMapping(value="/post/groundtruth", method=RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<String> saveGroundTruth(@RequestBody GroundTruthData groundTruth) throws SQLException{
        if(groundTruth.getAccessCode().trim().equalsIgnoreCase("test")){
            Connection connection = dataSource.getConnection();
            SqlQueries query = new SqlQueries(connection);
            query.setGroundTruth(groundTruth);
            return new ResponseEntity<String>(HttpStatus.OK);
	}else{
            return new ResponseEntity<String>(HttpStatus.UNAUTHORIZED);
        }		
    }
	
	
//  @RequestMapping(value="/greeting", method=RequestMethod.GET)
//  public String greetingForm(Model model) {
//      model.addAttribute("testsearch", new Search());
//      System.out.println("testsearch");
//      return "greeting";
//  }

//  @RequestMapping(value="/greeting", method=RequestMethod.POST)
//  public String greetingSubmit(@ModelAttribute Search search, Model model) throws SQLException {
//  	DataSourceConnection test = new DataSourceConnection();
//  	DataSourceConnection.sqlGetAll(search.getSearchMethod());
//      model.addAttribute("dave2", search);
//      System.out.println(search.getSearchMethod());
//      System.out.println("dave2");
//      return "result";
//  }
      
}
