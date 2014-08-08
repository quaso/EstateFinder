<%@taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="sk.kvaso.estate.db.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<jsp:useBean id="estates" type="java.util.List<Estate>" scope="request" />
<jsp:useBean id="lastScan" class="java.util.Date" scope="request" />
<jstl:url var="cssLink" value="/css/style.css" />
<jstl:url var="buttonJsLink" value="/scripts/css3-button.js" />

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Zoznam bytov</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<link rel="stylesheet" type="text/css" href="${cssLink}" />
</head>
<body>
	<form method="post">
		<button name="type" type="submit" class="action red" value="Delete">
			<span class="label">Delete</span>
		</button>
		<button name="type" type="submit" class="action blue"
			value="CollectNew">
			<span class="label">Collect new</span>
		</button>
		<span class="label">Total: ${fn:length(estates)}</span>
		<p class="time">
			<fmt:formatDate pattern="dd.MM.yyyy HH:mm:ss" value="${lastScan}"
				timeZone="Europe/Bratislava" />
		</p>
		<div class="clearer"></div>


		<jstl:forEach var="estate" items="${estates}">
			<div id="${estate.ID}" class="inzerat ">
				<div class="photo-border advertisement-photo">
					<a class="advertPhoto" href="${estate.URL}" target="_blank"> <img
						alt="${estate.TITLE}" data-src="${estate.THUMBNAIL}"
						src="${estate.THUMBNAIL}"></a>
				</div>

				<div class="advertisement-head ">
					<h2>
						<a href="${estate.URL}" target="_blank" title="${estate.ID}">${estate.TITLE}</a>
					</h2>
				</div>

				<div class="inzerat-content">
					<div class="locationText bold red">${estate.STREET}</div>
					<ul>
						<jstl:forEach var="note" items="${estate.NOTES}">
							<li class="advertisement-condition s">${note}</li>
						</jstl:forEach>
						<li class="estate-area s">Plocha: <span class="red">${estate.AREA}
								m²</span>
						</li>
					</ul>
					<p class="advertisement-content-p grey">${estate.SHORT_TEXT}</p>
				</div>

				<div class="advertisement-rightpanel">
					<p class="cena">
						<span class="tlste red"> ${estate.PRICE} € </span>
					</p>
					<p class="time">
						<span class="tlste"> <fmt:formatDate
								pattern="dd.MM.yyyy HH:mm" value="${estate.TIMESTAMP}"
								timeZone="Europe/Bratislava" />
						</span> <input name="selected" type="checkbox" value="${estate.ID}" />
					</p>
				</div>
				<div class="clearer"></div>
			</div>
		</jstl:forEach>
	</form>
</body>
</html>
