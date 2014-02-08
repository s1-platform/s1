var obj = {
    a:{b:{c:1},d:2},e:3
};
var obj2 = {};

function fn2(x,y){
    return x+"_"+y;
}

var fn1 = function(o){
    for(var p in o){
        if(typeof o[p] == "Map"){
            fn1(o[p]);
        }else if(typeof o[p] == "List"){
        }else{
            obj2[fn2(p,"1")] = o[p];
        }
    }
};


fn1(obj);

var a = "";
for(var p in obj2){
    a+=p+";";
}

assert("a1",s1.contains(a,"c_1;"));
assert("a2",s1.contains(a,"d_1;"));
assert("a3",s1.contains(a,"e_1;"));

//inline call;
var b = false;
(function(n){
    b = n==123;
})(123);
assert("b",b);

//arguments
var fn3 = function(){
    return s1.length(arguments)+":"+arguments[0]+":"+arguments[1];
};
assert("fn3",fn3(1,"2",3)=="3:1.0:2");