<%@ page info="a hello world example" %>
<%
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.addHeader("Content-Type", "text/html; charset=UTF8");
%>
<html><head>
      <title>Fedora:  403 Authz Error</title></head>
   <body>
      <center>
         <table border="0" cellpadding="0" cellspacing="0" width="784">
            <tbody><tr>
               <td height="134" valign="top" width="141"><img src="/images/newlogo2.jpg" height="134" width="141"></td>
               <td valign="top" width="643">
                  <center>
                     <h2>Fedora Security Error</h2>
                     <h3>authorization failed</h3>
                  </center>
               </td>
            </tr>
         </tbody></table>
      </center>
   </body></html>