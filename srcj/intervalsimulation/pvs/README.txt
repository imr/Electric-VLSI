            Formal Reasoning about asynchronous behaviour in PVS

PVS is Prototype Verification System. I tried to use it for verification of
asynchronoous circuits at behavoir level.

Behavior of asynchroneous circuts is described represented by sequence of events as
described in "Squaring FIFO in GasP" and "A Fast and Energy-Efficient Stack" papers.

The result is contained in 4 PVS files, which are in Electric CVS directory
"electric\srcj\intervalsimulation\pvs". They are
counts.pvs - facts about enumerating of subsets of natural numbers
traces.pvs - theory of traces
fsm.pvs    - automata and traces accepted by automata
fifo.pvs   - linear FIFO specification with proof of FIFO invariants

In more details

** counts.pvs

Following functions are defined
count(n: nat, S: set[nat]): nat  - returns number of elements of set "S" which are less than "n"
nth(S: set[nat])(n: nat): nat - returns nth element of set "S". "n" must be less than cardinal number of "S"


** traces.pvs

Type "trace[T: TYPE]" is a finite sequence of elements of type "T"
Let "t: trace[T]", "a: T", "k: nat" then
"t`length: nat" is a length of trace.
"t`seq(i): T" or simply "t(i): T" is i-th element of trace
"empty_trace: trace[T]" is a trace of length 0.
"add(t,a): trace[T]" is a result of appending element "a" to the right of "t"
"prefix(t, k): trace[T]" is a subtrace of "t" with length "k". "k" must be less than "t`length"
"prefix(t): trace[T]" is a prifix of "t" with length "t`length-1". "t" must be nonempty
"last(t)" is a last element of "t". "t" must be nonempty.

"add_induction" scheme is defined on traces:
  add_induction : PROPOSITION
     P(empty_trace) AND (FORALL t, a : P(t) IMPLIES P(add(t, a)))
	IMPLIES (FORALL t : P(t))

Let "A: pred[T]" is a predicate on T, "n: nat",
"count(t, A): nat" is a number of elements in "t" which for which satisfy "A"
"nth_index(t, A)(n): nat" is a index of n-th element of "t" which satisfies "A". "n" must be less than "count(t, A)"
"nth(t, A)(n): T" is n-th element of "t" which satisfies "A". "n" must be less than "count(t, A)"

"proj(t, A): trace[T]" is a subtrace of "t" which contains elements satisfying "A".


** fsm.pvs

This files cotains theory "fsm_def [event: TYPE+, state_opt: TYPE+, Reject: state_opt]: THEORY".
"event" is a set of events. "fsm" handles some subset of "event"s.
"state_opt" is a set consisting of states and special element "Reject".
"state: TYPE = { s: state_opt | s /= Reject }" is a set of states.
"fsm" is record:
  fsm: TYPE = [#
	event?: pred[event],                     % set of events of this fsm
	initial: state,                          % initial state
	nextstate: [state,(event?)->state_opt]   % transition table. "Reject" value indicates forbidden transitions
	#]

"nextstate(fs: fsm)(so: state_opt, e: event): state_opt" is extension of "nextstate" to all "event"s and to "Reject" element.
  When "e" is not in "event?", state is not changed.
"goto(fs: fsm, t: trace[T]): state_opt" is a state after sequence of events "t".
"goto(fs: fsm, t: trace[T], i: nat): state_opt" is a state after prefix of "t" with lenght "i".
"valid_trace(fs: fsm)(t: trace[T]): bool" - predicate which defines traces, accepted by "fs"

If Q(t: trace, s: state) is a predicate on trace and state, then induction scheme is defined

  fsm_induction: PROPOSITION
    Q(empty_trace, fs`initial) AND
    (FORALL t, s, (e: (fs`event?)):
       Q(t, s) AND fs`nextstate(s, e) /= Reject IMPLIES Q(add(t, e), fs`nextstate(s, e)))
    IMPLIES
      FORALL t: valid_trace(fs)(t) IMPLIES Q(proj(t, fs`event?), goto(fs, t)) 


** fifo.pvs

The traces and fsm theories are illustrated by example of linear "FIFO".

  data: TYPE+
  port: TYPE = upto(N)
  event: TYPE = [# port: port, data: data #]

"event" is a passing of "data" through "port". There are (N+1) ports, port 0 is input,
port N is output. "data" is unspecified type.

  fsm_name: TYPE = below(N)

There are N fsm's. The state of each fsm is defined by 

  state_opt: DATATYPE
  BEGIN
    Reject: Reject?
    E: E?
    F(d: data): F?
  END state_opt

"fsm" can be in an empty state "E" or in a full state "F(d)". There is a special element "Reject".
There are test functions "Reject?(so)", "E?(so)", "F?(so)".

"data_trace(t: trace[event]): trace[data]" this function maps trace of events into trace of their data.
"data_trace(p: port, t: trace[event]): trace[data]" this function returns trace of data, passed through port "p" in a trace "t"
"data_in(t: trace[event]): trace[data]" is data, passed through input port
"data_out(t: trace[event]): trace[data]" is data, passed through input port

Each of "fsm" is defined as follows:

  % Fsm "n" handles events with port "n" or "n+1"
  simple_event?(n)(e): bool = e`port = n OR e`port = n+1

  % Nextstate table for fsm "n"
  % E = (n,d) -> F(d)
  % F(d) = (n+1,d) -> E
  simple_nextstate(n)(s: state, e: (simple_event?(n))): state_opt =   
    CASES s OF
      E: IF e`port = n THEN F(e`data) ELSE Reject ENDIF,
      F(d): IF e`port = n+1 & e`data = d THEN E ELSE Reject ENDIF
    ENDCASES

  % Definition of fsm "n"
  simple_fsm(n): fsm =
    (#
      event? := simple_event?(n),
      initial := E,
      nextstate := simple_nextstate(n)
    #)


Then valid traces are those which are accepted by every automata:

  valid_trace?(t): bool = FORALL n: valid_trace(simple_fsm(n))(t)

  valid_trace: TYPE = (valid_trace?)


This ends the specification of linear FIFO. The following invariant
on each automata was proved by PVS

  simple_invariant(n)(t, s): bool = 
    IF even?(t`length) THEN
      s = E & data_trace(n, t) = data_trace(n + 1, t)
    ELSE
      s = F(last(t)`data) & last(t)`port = n & data_trace(n, t) = add(data_trace(n+1, t), last(t)`data)
    ENDIF

"t" is a projection of whole trace which contains only events { n, n+1 } handled by automata "n".
If there is even number of such events, than automata is in state "E" and "data" traces on ports
of this automata are matched. If there is odd number of such events, than automata is in state "F(d)"
where "d" is last event at input port of "fsm". "data" trace at output is one element shorter, than
"data" trace at input.

With the help of this invariant I proved that data trace at output port and at each internal port is
a prefix of data trace at input port:

  data_port_prefix_data_in: LEMMA data_trace(p, vt)`length <= data_in(vt)`length AND
    data_trace(p, vt) = prefix(data_in(vt), data_trace(p, vt)`length)

  data_out_prefix_data_in: LEMMA data_out(vt)`length <= data_in(vt)`length AND
    data_out(vt) = prefix(data_in(vt), data_out(vt)`length)


Future works.

1. Specify and prove property of "liveness".
   If "t" is trace with fixed input data trace "data_in(t)=DI", than
   this strace can't be infinitely contined:
    EXISTS (T: nat): FORALL (tc: trace[event]): data_in(tc)=DI & t = prefix(tc, t`length) IMPLIES tc`length <= t`length + T

2. Describe and prove square FIFO.

3. Specify behavoiur of stack. Describe and prove "linear", "n-place" and "tree" stacks.

4. PVS can prove design correctness of design "fifo[N]" for any all value of parameter "N", but it is hard to prove.
   Try to prove "fifo[16]" for fixed "N" using automatic model checker.

5. FIFO and stack are past designs. Formal reasoning can be used for new designs to verify project at earlier stage.








