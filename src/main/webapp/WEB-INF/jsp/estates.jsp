<%@taglib prefix="jstl" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="sk.kvaso.estate.db.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<jsp:useBean id="estates" type="java.util.List<Estate>" scope="request" />
<jsp:useBean id="lastScan" class="java.util.Date" scope="request" />

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Zoznam bytov</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />


<link href="css/bootstrap.min.css" rel="stylesheet" type="text/css">
	<link href="css/dashboard.css" rel="stylesheet" type="text/css">
</head>
<body>
	<form method="post">
		<div class="navbar navbar-default navbar-fixed-top" role="navigation">
			<div class="container">
				<div class="navbar-header">
					<button data-target=".navbar-collapse" data-toggle="collapse"
						class="navbar-toggle" type="button">
						<span class="sr-only">Toggle navigation</span> <span
							class="icon-bar"></span> <span class="icon-bar"></span> <span
							class="icon-bar"></span>
					</button>
					<a href="#" class="navbar-brand">Estate Finder</a>
				</div>
				<div class="navbar-collapse collapse">
					<ul class="nav navbar-nav">
						<li><a href="/collect">Collect new</a></li>
						<li><a href="/delete">Delete</a></li>
					</ul>
					<ul class="nav navbar-nav navbar-right">
						<li><a>Total: <span class="badge">${fn:length(estates)}</span></a></li>
						<li><a><fmt:formatDate pattern="dd.MM.yyyy HH:mm:ss"
									value="${lastScan}" timeZone="Europe/Bratislava" /></a></li>
					</ul>
				</div>
				<!--/.nav-collapse -->
			</div>
		</div>
		<!--		
		<ul class="nav nav-pills">
			<li><button name="type" type="submit" class="btn btn-info"
					value="CollectNew">
					<span class="label">Collect new</span>
				</button></li>
			<li><button name="type" type="submit" class="btn btn-danger"
					value="Delete">
					<span class="label">Delete</span>
				</button></li>
			<li>Total: <span class="badge">${fn:length(estates)}</span></li>
		</ul>
		 -->
		<p class="time"></p>
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
