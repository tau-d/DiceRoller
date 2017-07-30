package tau.david.mydiceroller;


class DiceSet {
    String name;
    long id;

    DiceSet(String name, long id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }
}
