package com.example;

import framework.ModelView;
import framework.annotations.Controller;
import framework.annotations.UrlMapping;

@Controller
public class EmpController {

    @UrlMapping("/emp/list")
    public ModelView liste() {
        ModelView mv = new ModelView("/views/emp.jsp");
        mv.addObject("title", "Liste des employés");
        mv.addObject("employees", java.util.List.of("Alice", "Bob", "Charlie"));
        return mv;
    }

    @UrlMapping("/emp/find")
    public ModelView find(@framework.annotations.Param("id") int id) {
        String name = "Employé #" + id;
        ModelView mv = new ModelView("/views/emp.jsp");
        mv.addObject("title", "Détail employé");
        mv.addObject("employees", java.util.List.of(name));
        return mv;
    }
}
