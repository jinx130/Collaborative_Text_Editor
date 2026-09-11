package com.yourname.editor.document;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serve the bundled frontend (src/main/resources/static/index.html) at "/".
 * The API remains under "/api/**".
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}
