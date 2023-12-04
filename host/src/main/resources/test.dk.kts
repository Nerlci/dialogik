import org.bupt.nerlci.dialogik.dsl.robot

robot() {
    var ctr = 0
    welcome {
        send("Hello")
    }
    before {
        send("Msg #$ctr: ")
        ctr += 1
    }
    receive("Hello") {
        send("World")
    }
    receive("World(.*)") {
        val name = message.getParam(1)
        send("Hello  " + name + "\n")
        for (i in 0..10) {
            send("Hello " + name + " " + i + "\n")
        }
    }
    receive() {
        send("I can't understand what you've said.")
    }
    goodbye {
        send("Goodbye")
    }
}.start()