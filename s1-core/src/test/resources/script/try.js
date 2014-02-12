var r = false;
var f = false;

try{
    throw "test";
}catch(e if e=='test'){
    r = true;
}catch(e){
}finally{
    f = true;
}

assert("1",r)
assert("2",f)


var r = false;
var f = false;

try{
    print("1");
    throw "test";
    print("2");
}catch(e){
    print(e);
    r = e;
}finally{
    print("3");
    f = true;
}

print(r)
assert("3",r=="test")
assert("4",f)