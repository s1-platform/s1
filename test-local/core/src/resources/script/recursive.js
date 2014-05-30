var closure = function(){
var c=1;
//recursion
var f = function(a){
    a++;
    var b = a;
    if(b<10){
        f(b);
    }
    print(a+":"+b);
    assert("check",a==b);
    assert("check2",c==1);
}
return f;
}

closure()(0);
