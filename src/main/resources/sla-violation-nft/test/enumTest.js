class Status {
    static OPEN = new Status("open");
    static SOLVED = new Status("solved");
    static REJECTED = new Status("rejected");
    static IN_PROGRESS = new Status("in_progress");

    constructor(value) {
        this.value = value;
    }

    isStatusValid() {
        return this.value == Status.OPEN.value || this.value == Status.SOLVED.value || this.value == Status.REJECTED.value || this.value == Status.IN_PROGRESS.value;
    }
}

const falseData= new Status(true);
const openData= new Status("open");
console.log(falseData.isStatusValid());
console.log(openData.isStatusValid());
console.log(openData.value);
console.log(falseData.value);