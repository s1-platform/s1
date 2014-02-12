var a = 1;
var b = 2;

{
    //block1
    var c = 3;
    assert("block1",c==3);
    {
        //block 2
        var d = 4;
        assert("block2",d==4);
    }
    print(d);
    assert("block1 d",!d);
}
assert("!c",!c);
assert("!d",!d);

assert("a",a==1);
assert("b",b==2);