package ie.ucd.serverjavafiles;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

	private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

	public static final String ROOT = "src/main/resources_scripts/data_cleaning/data_storage/new_data";

	private final ResourceLoader resourceLoader;

	@Autowired
	public FileUploadController(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@RequestMapping(method = RequestMethod.GET, value = "upload")
	public String provideUploadInfo(Model model) throws IOException {

		model.addAttribute("files", Files.walk(Paths.get(ROOT))
				.filter(new Predicate<Path>() {
					@Override
					public boolean test(Path path) {
						return !path.equals(Paths.get(ROOT));
					}
				})
				.map(new Function<Path, Object>() {
					@Override
					public Object apply(Path path) {
						return Paths.get(ROOT).relativize(path);
					}
				})
//				.map(new Function<Object, Object>() {
//					@Override
//					public Object apply(Object path) {
//						return linkTo(methodOn(FileUploadController.class).getFile(path.toString())).withRel(path.toString());
//					}
//				})
				.collect(Collectors.toList()));

		return "uploadForm";
	}

	
//	@RequestMapping(method = RequestMethod.GET, value = "/{filename:.+}")
//	@ResponseBody
//	public ResponseEntity<?> getFile(@PathVariable String filename) {
//
//		try {
//			return ResponseEntity.ok(resourceLoader.getResource("file:" + Paths.get(ROOT, filename).toString()));
//		} catch (Exception e) {
//			return ResponseEntity.notFound().build();
//		}
//	}

	@RequestMapping(method = RequestMethod.POST, value = "/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
								   RedirectAttributes redirectAttributes) {

		if (!file.isEmpty()) {
			try {
				Files.copy(file.getInputStream(), Paths.get(ROOT, file.getOriginalFilename()));
				redirectAttributes.addFlashAttribute("message",
						"You successfully uploaded " + file.getOriginalFilename() + "!");
				// Run upload script
				System.out.println("Upload starting");
				helpers.activateScript("python3", "data_cleaning", "data_input_manager.py");
				helpers.activateScript("Rscript", "dataAnalysis", "final_analyses.R");
				
			} catch (IOException|RuntimeException e) {
				redirectAttributes.addFlashAttribute("message", "Failued to upload " + file.getOriginalFilename() + " => " + e.getMessage());
			}
		} else {
			redirectAttributes.addFlashAttribute("message", "Failed to upload " + file.getOriginalFilename() + " because it was empty");
		}

		return "redirect:/upload";
	}
	
	public static void runProccessingScript() {
		// Activate data upload
		helpers.activateScript("python3", "data_cleaning/data_input_manager.py", "");
		
	}

}