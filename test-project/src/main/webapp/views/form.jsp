<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>${title}</title>
</head>
<body>
    <h1>${title}</h1>
    <form action="${pageContext.request.contextPath}/emp/save" method="post">
        <p>
            <label for="nom">Nom :</label>
            <input type="text" id="nom" name="nom" />
        </p>
        <p>
            <label for="age">Âge :</label>
            <input type="number" id="age" name="age" />
        </p>
        <p>
            <label for="email">Email :</label>
            <input type="text" id="email" name="email" />
        </p>
        <p>
            <button type="submit">Submit</button>
        </p>
    </form>
    <h2>Binding d'objet</h2>
    <form action="${pageContext.request.contextPath}/emp/save-personne" method="post">
        <p>
            <label for="personne.nom">Nom :</label>
            <input type="text" id="personne.nom" name="personne.nom" />
        </p>
        <p>
            <label for="personne.age">Âge :</label>
            <input type="number" id="personne.age" name="personne.age" />
        </p>
        <p>
            <button type="submit">Enregistrer l'objet</button>
        </p>
    </form>
</body>
</html>
