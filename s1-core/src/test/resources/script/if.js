var a = true;
var r = false;
if(a){
    r = true;
}
assert("1",r);


var r = false;
if(!a){

}else{
    r = true;
}
assert("2",r);

var r = false;
if(1==0){

}else if(1==2){

}else if(1==3){

}else{
    r = true;
}
assert("3",r);

var r = false;
if(1==0){

}else if(1==2){

}else if(1==1){
    r = true;
}else{

}
assert("4",r);

var r = false;
if(x){

}else{
    r = true;
}
assert("5",r);