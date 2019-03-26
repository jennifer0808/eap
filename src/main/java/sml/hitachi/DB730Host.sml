****************************************************************************
*  Filename    : DB730HOST.SML
*  Description : Pfile for the HOST side of the DB730
*  Author      : NJTZ
*  Date        : 03/02/2016
*
****************************************************************************
* Find out MAX_SVIDREQ, MAX_IMAGES & MAX_COMPARM values
#define MAX_COLEVENTS   100
#define MAX_VARIDS      50
#define MAX_COMPARM     30

****************************************************************************
* S?F0    Abort Transaction                                    E <-> H     *
****************************************************************************

S1F0 = s1f0out OUTPUT.

S1F0 = s1f0in INPUT.

S9F0 =s9f0out OUTPUT.

S9F0 =s9f0in INPUT.

****************************************************************************
* S1F1    Are You There/Online? (R)                            H <-> E     *
****************************************************************************

S1F1 = s1f1in INPUT W.

S1F1 = s1f1out OUTPUT W.

****************************************************************************
* S1F2    On Line Data (D)                                     E <-> H     *
****************************************************************************

S1F2 =s1f2out OUTPUT
  <L [0]>.

S1F2 =s1f2inNull INPUT
  <L [0]>.

S1F2 =s1f2in INPUT
  <L [2]
      <A [MAX 6] >               = Mdln
      <A [MAX 32] >               = SoftRev
  >.

S1F2 =s1f2outFull OUTPUT
  <L [2]
      <A [MAX 6] >               = Mdln
      <A [MAX 32] >               = SoftRev
  >.
****************************************************************************
* S1F3    Select Equipment Status Request                      E <- H      *
****************************************************************************
S1F3 =s1f3statecheck OUTPUT W
 <L [3]
    <U2 [1]> = EquipStatus
    <U2 [1]> = PPExecName
    <U2 [1]> = ControlState
>.

S1F3 = s1f3rcpandstate OUTPUT W
 <L [3]
    <U2 [1]>=EquipStatus
    <U2 [1]>=PreProcessStatus
    <U2 [1]>=PPExecName
>.

S1F3 =s1f3singleout OUTPUT W
<L [1]
    <U2 [1]> = SVID
>.
S1F3 = s1f3Specific OUTPUT W
<V>=DATA.

S2F13 = s2f13Specific OUTPUT W
       <V>=DATA.
****************************************************************************
* S1F4    Selected Equipment Status                       E -> H           *
****************************************************************************
S1F4 = s1f4statein INPUT
 <L [3]
    <U1 [1]> = ProcessState
    <A [MAX 100]> = ppName
    <U1 [1]> = Controlstate
>.
S1F4 =s1f4in INPUT 
  <V> = RESULT.
****************************************************************************
* S1F13   Establish Communications Request (CR)                H <-> E     *
****************************************************************************

S1F13 =s1f13in INPUT W
  <L [2]
    <A [MAX 6] >                     = Mdln
    <A [MAX 6] >                     = SoftRev
  >.
  
S1F13 = s1f13outListZero OUTPUT W
  <L [0]>.

S1F13 =s1f13outFull OUTPUT W
  <L [2]
    <A [MAX 6] >                     = Mdln
    <A [MAX 6] >                     = SoftRev
  >.

****************************************************************************
* S1F14   Establish Communications Request Acknowledge (CR)      H <-> E   *
****************************************************************************
  
S1F14 = s1f14in INPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 6] >                    = Mdln
      <A [MAX 16] >                    = SoftRev
    >
  >.

S1F14 = s1f14out OUTPUT
  <L [2]
      <B [1] >                     = AckCode
      <L [0] >
  >.

S1F14 = s1f14outFull OUTPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 6] >                    = Mdln
      <A [MAX 6] >                    = SoftRev
    >
  >.

S1F14 = s1f14inFull INPUT
  <L [2]
      <B [1] >                     = AckCode
      <L [0] >
  >.

****************************************************************************
* S5F1   Alarm Report Send (ARS)                               H <- E      *
****************************************************************************

S5F1 = s5f1in     INPUT
 <L [3]
    <B [1] >                            = ALCD
    <V>                            	= ALID
    <A [MAX 256]>                       = ALTX
 >.

****************************************************************************
* S5F2   Alarm Report Acknowledge (ARA)                        H -> E      *
****************************************************************************

S5F2 = s5f2out      OUTPUT
  <B [1] >      = AckCode.
****************************************************************************
* S5F3  Enable Alarm Send                    E <- H      *
****************************************************************************
S5F3 OUTPUT = s5f3enableAllout  W 
<L [2]
    < B [1] -128>
    <U4 [0]>
>.
S5F3 OUTPUT = s5f3disableAllout  W 
<L [2]
    <B [1] 127>
    <U4 [0]>
>.
S5F3 = s5f3out OUTPUT W 
<L[2]
    <B [1]> = ALED
    <U4 [1]> = ALID   
>.
****************************************************************************
* S5F4 Enable Alarm Report ACK                    E -> H         *
****************************************************************************
S5F4 = s5f4in INPUT 
    <B [1]> = AckCode.  
****************************************************************************
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************
 S6F11 = s6f11inStripMapUpload  INPUT W
 <L [3]
         <U1 [1] >                    = DataId
         <U1 [1] >                    = CollEventID
         <L [1]
           <L [2]
             <U2 [1] >                = ReportId
             <L [1]
               <V>                    = MapData
             >                    
           >
         >
  >.
   
S6F11 = s6f11equipstatuschange INPUT W
<L [3]
	<U1[1]>                   = DataId
	<U1[1]>                   = CollEventID
	<L [1]
		<L [2]
			<U2 [1] >   = ReportId
			<L [2]
                                <U1[1]>       = EquipStatus
				<A[MAX 40]>   = PPExecName
			>
		>
	>
>. 
S6F11 = s6f11incommon  INPUT W 
<L [3]
	<U1[1]>                   = DataId
	<U1[1]>                   = CollEventID
	<L [1]
		<L [2]
			<U1[1] >= ReportId 
			<L [1]
				<V>=DATA
			>
		>
	>
>. 
****************************************************************************
* S6F12  Event Report Acknowledge (ERA)                        H -> E      *
****************************************************************************

S6F12 OUTPUT = s6f12out
  <B [1]>  = AckCode.

S6F12 Input = s6f12in
  <B [1]>     = AckCode.
  

****************************************************************************
* S7F1    Process Program Load Inquire                          E<->H      *
****************************************************************************

S7F1  = s7f1out OUTPUT W
<L[2]
    <A [MAX 100] >             = ProcessprogramID
    <U4 [1]>                   = Length
>.

S7F1  = s7f1in INPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID
    <U4 [1]>                    = Length
>.

****************************************************************************
* S7F2   Process Program Load Grant                          E<->H         *
****************************************************************************
S7F2 OUTPUT = s7f2out
<B [1]> = PPGNT.

S7F2 INPUT = s7f2in
<B [1]> = PPGNT.

****************************************************************************
* S7F3   Process Program Send                                E<->H         *
****************************************************************************
S7F3  = s7f3out OUTPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <B [MAX 10000000]>                     =Processprogram
>.
S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <A [MAX 10000000] >                   =Processprogram
>.

****************************************************************************
* S7F4   Process Program Acknowledge                          E<->H        *
****************************************************************************
S7F4 OUTPUT = s7f4out
<B [1]> = AckCode.

S7F4 INPUT =s7f4in
<B [1]> = AckCode.

****************************************************************************
* S7F5  Process Program Request                                 E<->H      *
****************************************************************************
S7F5 OUTPUT = s7f5out W
<A [MAX 100] >= ProcessprogramID.

S7F5 INPUT = s7f5in W
<A [MAX 100] >= ProcessprogramID.

****************************************************************************
* S7F6  Process Program Data                                    H<->E      *
****************************************************************************

S7F6 OUTPUT = s7f6out
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                     = Processprogram
>.
S7F6 = s7f6in INPUT 
<L[2]
    <A [MAX 100] >               =  ProcessprogramID 
    <V>                     = Processprogram
>.
S7F6 = s7f6zeroin INPUT 
<L[0]   
>.
****************************************************************************
* S7F9 M/P  M Request(MMR)                                     E<-> H      *
****************************************************************************
S7F9 INPUT = s7f9in  W.

S7F9 OUTPUT = s7f9out  W.

****************************************************************************
* S7F10 M/P  M Data(MMR)                                       E<-> H      *
****************************************************************************
S7F10 = s7f10in INPUT 
<L[1]
    <L[2]
    <A [MAX 100]> = ProcessprogramID
    <L[1]
    <A [MAX 100]> = MaterialID
    >
    >
>.
****************************************************************************
* S7F17 Delete Process Program Send                              H->E      *
****************************************************************************
S7F17  = s7f17out OUTPUT W
<L [1]
    <A [MAX 100]>                = ProcessprogramID 
>.

****************************************************************************
* S7F18 Delete Process Program Acknowledge                       E->H      *
****************************************************************************
S7F18 INPUT = s7f18in
<B [1]> = AckCode.

****************************************************************************
* S7F19 Current EPPD Request                                     H->E      *
****************************************************************************
S7F19  = s7f19out OUTPUT W.

S7F19 = s7f19in INPUT W.
****************************************************************************
* S7F20 Current EPPD Data                                        E->H      *
****************************************************************************
S7F20 INPUT = s7f20in

    <V>                = EPPD.

****************************************************************************
* S7F21 Equipment Process Cabpabiloties Request                  E<-H      *
***************************************************************************
S7F21 = s7f21in INPUT W.
S7F21 = s7f21out OUTPUT W.
****************************************************************************
* S7F22 Equipment Process Cabpabiloties Data                  E->H      *
***************************************************************************
S7F22 =s7f22in INPUT
<L[5]
    <A [ MAX 6] > = Mdln
    <A [MAX 6] > = SoftRev
    <U1 [1]> = CommandMax
    <U4 [1]> = BtyeMax
    <L[1]
        <L[11]
           <U2 [1]> =  Commandcode
            <A [MAX 16]> = Commandname
            <BOOLEAN [1]> = RequiedCommand
            <I1 [1]> = Blockdefinition 
            <I2 [1]> = BeforeCommandCodes
            <U2 [1]> = ImmediatelyBeforeCommandCodes
            <U2 [1]> = NotBeforeCommandCodes
            <U2 [1]> = AfterCommandCodes
            <U2 [1]> = ImmediatelyAfterCommandCodes
            <U2 [1]> = NotAfterCommandCodes
            <L [9]
                <A [MAX 16]> = ParameterName
                <BOOLEAN [1]> = RequiedParameter
                <F8 [1] > = ParameterDefaultValue
                <U2 [1] > = ParameterCountMaximum
		<F8 [1] > =LowerLimitForNumericValue
		<F8 [1] > =UpperLimitForNumericValue
		<A [MAX 10] > =UnitsIdentifier
		<I1 [1] > =ResolutionCodeForNumericData
		<F8 [1] > =ResolutionValueForNumericData
            >
        >
    >
>.
 ****************************************************************************
* S7F25 Formatted Process Program Requet                        E<-> H     *
****************************************************************************
S7F25  = s7f25in INPUT  
<A [MAX 100]> = ProcessprogramID.
S7F25 OUTPUT = s7f25out   W
<A [MAX 100]> = ProcessprogramID.


****************************************************************************
* S7F26 Formatted Process Program Request                     E< -> H      *
****************************************************************************
S7F26 = s7f26onein INPUT
<L[4]
    <A [MAX 100] > = ProcessprogramID
    <A [MAX 6] > = Mdln
    <A [MAX 6] > = SoftRev
    <L[1]
        <L[2]
     <A [MAX 90] >  =Commandcode
        <L[1]
      <V>      = ProcessParameter
        >
    >
>
>.
S7F26 =s7f26in INPUT
<V>=RESULT.

****************************************************************************
* S7F27  Process Program Verification Send                    E -> H      *
****************************************************************************
S7F27 = s7f27in INPUT W
<L[2]
<A [MAX 100] > = ProcessprogramID
    <L[1]
        <L[3]
        <B [1]> =AckCode
        <U1 [1]> = CommandNumber
        <v> = ERRW7
        >
    >
>.
****************************************************************************
* S7F28  Process Program Verification Acknowledge              E< - H      *
****************************************************************************
S7F28 = s7f28out OUTPUT. 
****************************************************************************
* S7F33  Process Program Available Request             E<-> H      *
****************************************************************************
S7F33 = s7f33out OUTPUT W
<L[1]
<A [MAX 100] > = ProcessprogramID
>.

****************************************************************************
* S14F1  GetAttr Request (GAR)                               H  <-  E      *
****************************************************************************
S14F1 INPUT = s14f1in W
<L [5]
    <A [0]>
    <A 'StripMap'>
    <L [1]
      <A [MAX 32]>         =  StripId
    >
    <L [1]
      <L [3]
         <A 'SubstrateType'>
         <A 'Strip'>
         <U1 0>
      >
    > 
    <L [0]>
>.

****************************************************************************
* S14F2  GetAttr Data (GAD)                                   H <-> E      *
****************************************************************************
S14F2 OUTPUT = s14f2out
<L [2]
    <L [1]
      <L [2]
        <A [MAX 32]>              = StripId
        <L [1]
        	<L[2]
        	   <A  'MapData'> 
	           <A  [MAX 12000]>    = MapData
	        >
	    >
	  >
	>
    <L [2]
      <U1 0>
      <L [0]>
    >
>.

S14F2 OUTPUT = s14f2outException
<L [2]
    <L [1]
      <L [2]
        <A [MAX 80]>              = StripId
        <L [1]
          <L [2]
             <A  'MapData'> 
	         <A  [MAX 12000]>    = MapData
          >
        >
      >
    >
    <L [2]
      <U1 1> 
      <L [1]
        <L [2]
           <U1 [1]>              = ErrCode
           <A [MAX 80]>          = ErrText
        >
      >
    >
>.
S14F2 OUTPUT = s14f2outNoExist
<L [2]
    <L [0]>
    <L [2]
      <U1 [1]>                    = ObjectAck
      <L [0]>
    >
>.
****************************************************************************
* S14F3  SetAttr Request (SAR)                               H  <-  E      *
****************************************************************************
S14F3 INPUT = s14f3in W
<L [4]
    <A[0]>
    <A 'StripMap'>
    <L [0]
    >
    <L [4]
      <L [2]
         <A 'Orientation'>
         <A '0'>
      >
      <L [2]
         <A 'OriginLocation'>
         <A 'UpperRight'>
      >
      <L [2]
         <A 'SubstrateSide'>
         <A 'TopSide'>
      >
      <L [2]
         <A 'AxisDirection'>
         <A 'DownLeft'>
      >
    >
>.
****************************************************************************
* S14F4  SetAttr Data (SAD)                                   H <-> E      *
****************************************************************************
S14F4 OUTPUT = s14f4out
<L [2]
    <L [1]
        <L [2]
            <A [0]>
            <L [4]
              <L [2]
                 <A 'Orientation'>
                 <A '0'>
              >
              <L [2]
                 <A 'OriginLocation'>
                 <A 'UpperRight'>
              >
              <L [2]
                 <A 'SubstrateSide'>
                 <A 'TopSide'>
              >
              <L [2]
                 <A 'AxisDirection'>
                 <A 'DownLeft'>
              >
            >
        >
    >
    <L [2]
        <U1 0>
        <L[0]>
    >
>. 

****************************************************************************
*S2F41 Host Command Send(HCS)                                         H->E *
****************************************************************************
S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP-SELECT'> 
    <L[1]
        <L[2]
            <A 'PPROGRAM'> 
            <A [MAX 50]> = PPID
        >
    >
>.
S2F41  = s2f41outSTART OUTPUT W
<L[2]
    <A 'START'> 
    <L[1]
        <L[2]
            <A ''> 
            <A ''> 
        >
    >
>.
S2F41  = s2f41outSTOP OUTPUT W
<L[2]
    <A 'STOP'> 
    <L[1]
        <L[2]
            <A ''> 
            <A ''> 
        >
    >
>.
S2F41  = s2f41outPAUSE OUTPUT W
<L[2]
    <A 'PAUSE'> 
    <L[1]
        <L[2]
            <A ''> 
            <A ''> 
        >
    >
>.
S2F41  = s2f41outRESUME OUTPUT W
<L[2]
    <A 'RESUME'> 
    <L[1]
        <L[2]
            <A ''> 
            <A ''> 
        >
    >
>.
S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L[1]
        <L[2]
            <A ''> 
            <A ''> 
        >
    >
>.
S2F41  = s2f41outConfig OUTPUT W
<L[2]
    <A [MAX 50]> = RCMD
    <V> = CPLIST
>.
****************************************************************************
*S2F42 Host Command Send(HCS)                                         H<-E *
****************************************************************************
S2F42 = s2f42in INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[0]
    >
>.
****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************

S2F33 OUTPUT = s2f33out W
<L [2]
    <U1 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>              =ReportID
            <L[1]
               <U2 [1] >          = VariableID            
            >
        >        
    >
>.
S2F33 OUTPUT = s2f33eqpstate W
<L [2]
    <U2 [1]>=DataID
    <L[1]
        <L [2]
            <U2 [1]>            =ReportID
            <L[2]              
               <U2 [1]>         = EquipStatusID 
               <U2 [1]>         = PPExecNameID     
              
            >
        >        
    >
>.
****************************************************************************
* S2F34 Define Report Acknowledge (DRA)                        H <- E      *
****************************************************************************

S2F34 INPUT = s2f34in 

<B[1]>=AckCode.


****************************************************************************
* S2F35    Link Event Report (LER)                             H -> E      *
****************************************************************************
S2F35 OUTPUT = s2f35out W
<L[2]
    <U1 [1]> = DataID
     <L[1]
        <L [2]
            <U2 [1]> = CollEventID
            <L[1]
                <U2 [1]> = ReportID
            >
        >
    >
>.

****************************************************************************
* S2F36   Link Event Report Acknowledge(LERA)                  E -> H      *
****************************************************************************

S2F36 INPUT = s2f36in 
<B [1]> = AckCode.


****************************************************************************
* S2F37   Enable / Disable Event Report (EDER)                 H -> E      *
****************************************************************************
S2F37 OUTPUT = s2f37out W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[1]
        <U2 [1]> = CollEventId
    >
>.
S2F37 OUTPUT = s2f37outAll W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[0]
    >
>.
S2F37 OUTPUT = s2f37outMuilt W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <V> = CEIDList
>.
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************
S2F38 INPUT = s2f38in 
<B [1]> = AckCode.
****************************************************************************
* S9F9 TractionTimeOut      E -> H      *
****************************************************************************
S9F9 INPUT = s9f9Timeout
<B[MAX 20]>.
****************************************************************************
* S10F1  Terminal request                                     E -> H      *
****************************************************************************
S10F1  = s10f1in  INPUT W
<L [2]
        <V>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F2 output = s10f2out
 <B [1]>        = AckCode
.

S10F3 output = s10f3out W
<L [2]
         <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F4 input = s10f4out
 <B [1]>        = AckCode
. 
   
  
