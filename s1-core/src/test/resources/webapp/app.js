"use strict";

$(document).ready(function(){
    $('#tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    });

    //monitor
    $("#refresh-monitor").click(function(){
        $(".nodes-count").html("?");
        $("#monitor-data").html("");
        $("#refresh-monitor").attr("disabled",true).find(">i").addClass("fa-spinner");
        $.ajax("dispatcher/Monitor.getClusterInfo",{
            dataType:"json",
            contentType:"application/json",
            data:JSON.stringify({}),
            type:"POST",
            success:function(data,status,xhr){
                $("#refresh-monitor").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#monitor-data").html(JSON.stringify(data,null,"  "));
                if(data.success){
                    $(".nodes-count").html(data.data.nodes.length);
                }
            },
            error:function(xhr,status,err){
                $("#refresh-monitor").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#monitor-data").html("Error: "+err);
                console.error(err);
            }
        });
    });

    //request
    $("#request-run").click(function(){
        $("#request-response").html("");
        $("#request-run").attr("disabled",true).find(">i").addClass("fa-spinner");
        var url = $("#request-url").val();
        var data = {};
        try{
            data = eval("("+$("#request-data").val()+")");
        }catch(e){
            $("#request-run").attr("disabled",false).find(">i").removeClass("fa-spinner");
            $("#request-response").html("Data parse error: "+e);
            return;
        }
        $.ajax("dispatcher/"+url,{
            dataType:"json",
            contentType:"application/json",
            data:JSON.stringify(data),
            type:"POST",
            success:function(data,status,xhr){
                $("#request-run").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#request-response").html(JSON.stringify(data,null,"  "));
            },
            error:function(xhr,status,err){
                $("#request-run").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#request-response").html("Error: "+err);
                console.error(err);
            }
        });
    });

});