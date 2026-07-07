<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>${title}</title>
</head>
<body>
    <h1>${title}</h1>
    <ul>
        <c:forEach var="emp" items="${employees}">
            <li>${emp}</li>
        </c:forEach>
    </ul>
</body>
</html>
