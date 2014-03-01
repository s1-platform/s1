"use strict";

$(document).ready(function(){
    $('#tabs a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    });

    var accounts = [];
    var nodes = [];

    //monitor
    $("#refresh-monitor").click(function(){
        nodes = [];
        $(".nodes-count").html("?");
        $("#monitor-data").html("");
        $("#refresh-monitor").attr("disabled",true).find(">i").addClass("fa-spinner");
        $.ajax("dispatcher/Monitor.clusterInfo",{
            dataType:"json",
            contentType:"application/json",
            data:JSON.stringify({}),
            type:"POST",
            success:function(data,status,xhr){
                $("#refresh-monitor").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#monitor-data").html(JSON.stringify(data,null,"  "));
                if(data.success){
                    nodes = data.data.nodes;
                    $(".nodes-count").html(nodes.length);
                }
            },
            error:function(xhr,status,err){
                $("#refresh-monitor").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#monitor-data").html("Error: "+err);
                console.error(err);
            }
        });
    });

    //accounts
    $("#refresh-accounts").click(function(){
        $("#accounts-data").html("");
        $("#operations-data").html("");
        accounts = [];
        $(".accounts-count").html("?");
        $("#refresh-accounts").attr("disabled",true).find(">i").addClass("fa-spinner");
        $.ajax("dispatcher/Command",{
            dataType:"json",
            processData:false,
            contentType:"application/json",
            data:JSON.stringify({list:[
                {operation:"Table",method:"aggregate",params:{table:"accounts",field:"balance"}},
                {operation:"Table",method:"aggregate",params:{table:"operations",field:"sum"}},
                {operation:"Table",method:"list",params:{max:0,skip:0,table:"accounts"}}
            ]}),
            type:"POST",
            success:function(data,status,xhr){
                $("#refresh-accounts").attr("disabled",false).find(">i").removeClass("fa-spinner");
                var dt = data;
                var dt2 = "";
                if(data.success){
                    dt = data.data.list[0].data;
                    dt2 = data.data.list[1].data;
                    accounts = data.data.list[2].data.list;
                    $(".accounts-count").html(accounts.length);
                }
                $("#accounts-data").html(JSON.stringify(dt,null,"  "));
                $("#operations-data").html(JSON.stringify(dt2,null,"  "));
            },
            error:function(xhr,status,err){
                $("#refresh-accounts").attr("disabled",false).find(">i").removeClass("fa-spinner");
                $("#accounts-data").html("Error: "+err);
                console.error(err);
            }
        });
    });

    //clear
    $("#clear").click(function(){
        if(nodes.length==0){
            console.error("Nodes.length == 0");
            return;
        }
        accounts = [];
        $(".accounts-count").html("?");
        $("#clear").attr("disabled",true).find(">i").addClass("fa-spinner");

        var res = [];
        for(var i=0;i<nodes.length;i++){
            var url = "http://"+nodes[i].address+":"+9000+"/s1/dispatcher/Test.clear";
            var result = function(dt){
                res.push(1);
            };

            $.getJSON(url+"?_callback=?", null, result);
        }
        var f = function(){
            if(res.length==nodes.length){
                $("#clear").attr("disabled",false).find(">i").removeClass("fa-spinner");
            }else{
                window.setTimeout(f,1000);
            }
        };
        window.setTimeout(f,1000);
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

    //tests
    var getRandomNode = function(){
        var i = Math.floor((Math.random()*1000000)%nodes.length);
        return nodes[i]
    };
    var getRandomAccount = function(){
        var i = Math.floor((Math.random()*1000000)%accounts.length);
        return accounts[i];
    };

    var callTest = function(res,i,test){
        var t = new Date().getTime();
        var url = "http://"+getRandomNode().address+":"+9000+"/s1/dispatcher/";
        var data = {table:"accounts"};
        if(test=="get"){
            url+="Table.get";
            data.id = getRandomAccount().id;
        }else if(test=="add"){
            url+="Table.changeState";
            data.action = "add";
            data.data = {
                title: "account_"+i,
                balance: 1000
            };
        }else if(test=="complex"){
            url+="Test.pay";
            var src = null;
            while(!src){
                src = getRandomAccount();
                if(src.balance<=0)
                    src = null;
            }
            var dest = null;
            while(!dest){
                dest = getRandomAccount();
                if(dest.id == src.id)
                    dest = null;
            }
            data.sum = 1;
            data.src = src.id;
            data.dest = dest.id;
        }
        url+="?_params="+encodeURIComponent(JSON.stringify(data));

        var testResult = function(dt){
            res.push({
                i:i,
                success:dt.success,
                time:new Date().getTime() - t
            });
        };

        $.getJSON(url+"&_callback=?", null, testResult);
    };

    var runTest = function(test){
        $("#run-"+test).click(function(){
            var count = $("#count-"+test).val()*1;
            if(count<=0){
                console.error("Test count <= 0");
                return;
            }
            if(nodes.length == 0){
                console.error("Nodes.length == 0");
                return;
            }
            if(test=="get" || test=="complex"){
                if(accounts.length == 0){
                    console.error("Accounts.length == 0");
                    return;
                }
            }

            $("#test-result-"+test).html("");
            $("#run-"+test).attr("disabled",true).find(">i").addClass("fa-spinner");

            var res = [];
            for(var i=0;i<count;i++){
                callTest(res,i,test);
            }

            var f = function(){
                if(res.length==count){
                    var sum = 0;
                    var min = 1000000;
                    var max = 0;
                    var success = 0;
                    for(var i=0;i<res.length;i++){
                        sum+=res[i].time;
                        if(max<res[i].time)
                            max = res[i].time;
                        if(min>res[i].time)
                            min = res[i].time;
                        if(res[i].success)
                            success++;
                    }
                    var aggr = {
                        total:res.length,
                        success:success,
                        error:res.length-success,
                        time_avg:sum/res.length,
                        time_min:min,
                        time_max:max
                    };
                    $("#test-result-"+test).html(JSON.stringify(aggr,null,"  "));
                    $("#run-"+test).attr("disabled",false).find(">i").removeClass("fa-spinner");
                }else{
                    window.setTimeout(f,1000);
                }
            };
            window.setTimeout(f,1000);
        });
    };
    runTest("add");
    runTest("get");
    runTest("complex");
});