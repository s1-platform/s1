Hello, {{s1.get(test,'name','anonymous')}}!!!
<%
var tasks = ["test1","test2","test3"];
%>
Your tasks are:
<ul class="tasks">
    <%for(var i in tasks){%><li>{{i+1}}: {{tasks[i]}}</li>
    <%}%>
</ul>