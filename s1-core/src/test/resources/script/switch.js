var a = "qwer";

var r = false;
switch(a){
    case "a":
        break;
    case "qwer":
        r = true;
        break;
    default:
        break;
}
assert("1",r);

var r = false;
switch(a){
    case "a":
        break;
    case "b":
        break;
    default:
        r = true;
        break;
}
assert("2",r);