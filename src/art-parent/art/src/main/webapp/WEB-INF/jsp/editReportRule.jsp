<%-- 
    Document   : editReportRule
    Created on : 27-Apr-2014, 14:37:13
    Author     : Timothy Anyona

Edit report rule page
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page trimDirectiveWhitespaces="true" %>

<%@taglib tagdir="/WEB-INF/tags" prefix="t" %>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@taglib uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project" prefix="encode" %>

<c:choose>
	<c:when test="${action == 'add'}">
		<spring:message code="page.title.addReportRule" var="pageTitle"/>
	</c:when>
	<c:when test="${action == 'edit'}">
		<spring:message code="page.title.editReportRule" var="pageTitle"/>
	</c:when>
</c:choose>

<spring:message code="select.text.noResultsMatch" var="noResultsMatchText" javaScriptEscape="true"/>

<t:mainPageWithPanel title="${pageTitle}" mainColumnClass="col-md-6 col-md-offset-3">
	
	<jsp:attribute name="javascript">
		<script type="text/javascript">
			$(document).ready(function() {
				$('a[id="configure"]').parent().addClass('active');
				$('a[href*="reportsConfig"]').parent().addClass('active');

				//{container: 'body'} needed if tooltips shown on input-group element or button
				$("[data-toggle='tooltip']").tooltip({container: 'body'});

				//Enable Bootstrap-Select
				$('.selectpicker').selectpicker({
					liveSearch: true,
					noneResultsText: '${noResultsMatchText}'
				});

				//activate dropdown-hover. to make bootstrap-select open on hover
				//must come after bootstrap-select initialization
				initializeSelectHover();

			});
		</script>
	</jsp:attribute>

	<jsp:attribute name="abovePanel">
		<div class="text-right">
			<a href="${pageContext.request.contextPath}/docs/Manual.html#rules">
				<spring:message code="page.link.help"/>
			</a>
		</div>
	</jsp:attribute>

	<jsp:body>
		<spring:url var="formUrl" value="/saveReportRule"/>
		<form:form class="form-horizontal" method="POST" action="${formUrl}" modelAttribute="reportRule">
			<fieldset>
				<c:if test="${formErrors != null}">
					<div class="alert alert-danger alert-dismissable">
						<button type="button" class="close" data-dismiss="alert" aria-hidden="true">x</button>
						<spring:message code="page.message.formErrors"/>
					</div>
				</c:if>
				<c:if test="${error != null}">
					<div class="alert alert-danger alert-dismissable">
						<button type="button" class="close" data-dismiss="alert" aria-hidden="true">x</button>
						<p><spring:message code="page.message.errorOccurred"/></p>
						<c:if test="${showErrors}">
							<p>${encode:forHtmlContent(error)}</p>
						</c:if>
					</div>
				</c:if>

				<input type="hidden" name="action" value="${action}">
				<input type="hidden" name="reportId" value="${reportId}">

				<div class="form-group">
					<div class="col-md-12 text-center">
						<b><spring:message code="page.text.report"/>:</b> ${encode:forHtmlContent(reportName)}
					</div>
				</div>

					<div class="form-group">
					<label class="control-label col-md-4">
						<spring:message code="page.label.id"/>
					</label>
					<div class="col-md-8">
						<c:if test="${action == 'edit'}">
							<form:input path="reportRuleId" readonly="true" class="form-control"/>
						</c:if>
					</div>
				</div>
				<div class="form-group">
					<label class="col-md-4 control-label " for="rule">
						<spring:message code="page.text.rule"/>
					</label>
					<div class="col-md-8">
						<form:select path="rule" class="form-control selectpicker">
							<form:options items="${rules}" itemLabel="name" itemValue="ruleId"/>
						</form:select>
						<form:errors path="rule" cssClass="error"/>
					</div>
				</div>
				<div class="form-group">
					<label class="col-md-4 control-label " for="reportColumn">
						<spring:message code="page.text.reportColumn"/>
					</label>
					<div class="col-md-8">
						<div class="input-group">
							<form:input path="reportColumn" maxlength="100" class="form-control"/>
							<spring:message code="rules.help.reportColumn" var="help"/>
							<span class="input-group-btn" >
								<button class="btn btn-default" type="button"
										data-toggle="tooltip" title="${help}">
									<i class="fa fa-info"></i>
								</button>
							</span>
						</div>
						<form:errors path="reportColumn" cssClass="error"/>
					</div>
				</div>
				<div class="form-group">
					<div class="col-md-12">
						<button type="submit" class="btn btn-primary pull-right">
							<spring:message code="page.button.save"/>
						</button>
					</div>
				</div>
			</fieldset>
		</form:form>
	</jsp:body>
</t:mainPageWithPanel>
