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

    @framework.annotations.Get("/emp/add")
    public ModelView addForm() {
        ModelView mv = new ModelView("/views/form.jsp");
        mv.addObject("title", "Ajouter un employé");
        return mv;
    }

    @framework.annotations.Post("/emp/save")
    public String save(String nom, int age, String email) {
        return "<h1>Employé enregistré</h1>"
                + "<ul>"
                + "<li>Nom : " + nom + "</li>"
                + "<li>Âge : " + age + "</li>"
                + "<li>Email : " + email + "</li>"
                + "</ul>"
                + "<a href=\"add\">Nouvel ajout</a>";
    }

    @framework.annotations.Post("/emp/save-personne")
    public String savePersonne(Personne personne) {
        return "<h1>Employé enregistré</h1>"
                + "<p>Nom : " + personne.getNom() + "</p>"
                + "<p>Âge : " + personne.getAge() + "</p>";
    }
}
