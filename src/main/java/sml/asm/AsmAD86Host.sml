****************************************************************************
*  Filename    : AD8312HOST.SML
*  Description : Pfile for the HOST side of the ASM AD8312
*  Author      : hecongyuan
*  Date        : 03/25/2016
*  Modify      : luosy
*  Modify date : 02/20/2017
****************************************************************************
* Find out MAX_SVIDREQ,  MAX_IMAGES & MAX_COMPARM values
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
  <L [2]
    <A [MAX 32] >               = Mdln
    <A [MAX 32] >               = SoftRev
  >.

S1F2 =s1f2in INPUT
  <L [2]
      <A [MAX 32] >               = Mdln
      <A [MAX 32] >               = SoftRev
  >.
  
  S1F2 =s1f2out838 OUTPUT
  <L [0]
  >.

****************************************************************************
* S1F3  Selected Equipment Status Request (SSR)                H  -> E     *
****************************************************************************

S1F3 = s1f3statecheck OUTPUT W
 <L [3]
    <U4 [1]> = EquipStatus
    <U4 [1]> = PPExecName
    <U4 [1]> = ControlState
>.

****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************
S1F4 =s1f4in INPUT 
<V> = RESULT.
 
****************************************************************************
* S1F13   Establish Communications Request (CR)                H <-> E     *
****************************************************************************

S1F13 =s1f13in INPUT W
  <L [2]
    <A [MAX 32] >                     = Mdln
    <A [MAX 32] >                     = SoftRev
  >.
  
S1F13 = s1f13out OUTPUT W
  <L [2]
    <A [MAX 32] >                     = Mdln
    <A [MAX 32] >                     = SoftRev
  >.

S1F13 = s1f13outListZero OUTPUT W
  <L [0]>.

****************************************************************************
* S1F14   Establish Communications Request Acknowledge (CR)      H <-> E   *
****************************************************************************

S1F14 = s1f14in INPUT
  <L [2]
    <B [1] >                     = AckCode
    <L [0] >
  >.
  
S1F14 = s1f14out OUTPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 32] >                    = Mdln
      <A [MAX 32] >                    = SoftRev
    >
  >.
  
  S1F14 = s1f14inAMS INPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 32] >                    = Mdln
      <A [MAX 32] >                    = SoftRev
    >
  >.

S1F14 = s1f14outZero OUTPUT
  <L [2]
      <B [1] >                     = AckCode
      <L [0] >
  >.
  
****************************************************************************
* S1F15   Request OFF-LINE(ROFL)                                 H  -> E   *
****************************************************************************

S1F15 = s1f15out OUTPUT W.

S1F15 = s1f15in INPUT W.

****************************************************************************
* S1F17   Request ON-LINE (RONL)                                H  -> E   *
****************************************************************************

S1F17 = s1f17out OUTPUT W.

S1F17 = s1f17in INPUT W.

****************************************************************************
* S2F15   New Equipment Constant Send (ECS)                      H  -> E   *
****************************************************************************
S2F15 = s2f15out OUTPUT  W
  <L [2]
     <L[2]
         <U2 [1]>                        = EC096
         <A[MAX 60] >                   = NextLotId
     >
     <L[2]
         <U2 [1]>                        = EC097
         <U4 [1]>                        = NextLotQuantity
     >
  >.

S2F16 = s2f16in   INPUT
  <B [1]>                       = EAC.

****************************************************************************
* S2F33    Define Report (DR)                                  H -> E      *
****************************************************************************

S2F33 = s2f33out OUTPUT W
<L [2]
    <U2 [1]>=DateID
    <L[1]
        <L [2]
            <U2 [1]>=ReportId031
            <L[1]
               <U2 [1] >          = EquipmentId             
            >
        >    
    >
>.

****************************************************************************
* S2F34 Define Report Acknowledge (DRA)                        H <- E      *
****************************************************************************

S2F34 = s2f34in INPUT

<B[1]> = AckCode.

****************************************************************************
* S2F35    Link Event Report (LER)                             H -> E      *
****************************************************************************
S2F35 = s2f35out OUTPUT    W
<L[2]
    <U1 [1]> = DateID
     <L[1]
        <L [2]
            <U2 [1]> = CollEventId032
            <L[1]
                <U2 [1]> = ReportId032
            >
        >
        
    >
>.
****************************************************************************
* S2F36   Link Event Report Acknowledge(LERA)                  E -> H      *
****************************************************************************

S2F36 = s2f36in INPUT
<B [1]> = AckCode.

****************************************************************************
* S2F37   Enable / Disable Event Report (EDER)                 H -> E      *
****************************************************************************

S2F37 = s2f37out OUTPUT 
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[1]
    <U2 [1]> = CollEventId032
    >
>.

S2F37 OUTPUT = s2f37outAll W
<L[2]
    <BOOLEAN [1]> = Booleanflag
    <L[0]
    >
>.
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************

S2F38 = s2f38in INPUT
<B [1]> = AckCode.

****************************************************************************
* S2F39   Multi-block Inquire (DMBI)                           H -> E      *
****************************************************************************
S2F39 = s2f39out OUTPUT
<L[2]
    <U1 [1]> = DataID
    <U1 [MAX 3000]> = Datelength
>.

****************************************************************************
* S2F40  Multi-block Grant (DMBG)                              E -> H      *
****************************************************************************

S2F40 = s2f40in INPUT
<B [1]>  = GrantCode.

****************************************************************************
* S2F41   Host Command Send (HCS)                              H -> E      *
****************************************************************************
S2F41 = s2f41outAD838 OUTPUT W
<L [2]
    <A [MAX 40] >                = RCMD
    <L [2]
      <L [2]
        <A [MAX 40] >            = CPnameResult
        <V>                      = CPvalResult  *U1/U2/U4/ascii 0 = pass, 1 = fail
      >
      <L [2]
        <A [MAX 40] >            = CPnameErrorMessage
        <A [MAX 256]>            = CPvalErrorMessage
      >
    >
  >.
  
S2F41 = s2f41outAD830 OUTPUT W
<L [2]
    <A [MAX 40] >                = RCMD
    <L [4]
      <L [2]
        <A [MAX 40] >            = CPnameResult
        <V>                      = CPvalResult  *U1/U2/U4/ascii 0 = pass, 1 = fail
      >
      <L [2]
        <A [MAX 40] >            = CPnameErrorMessage
        <A [MAX 256]>            = CPvalErrorMessage
      >
      <L [2]
        <A [MAX 40]>             = CPnameStripCount
        <A [MAX 4]>              = CPvalStripCount
      >
      <L [2]
        <A [MAX 40] >            = CPnameLastFlag
        <A [1]>            		 = CPvalLastFlag
      >
    >
  >.

S2F41 = s2f41stopStart  OUTPUT W
  <L [2]
    <A [MAX 40] >          = RemComCode
    <L [1]
      <L [2]
        <A [0]>            = CPname
        <A [0]>            = CPval
      >
    >
  >.
  
S2F41 = s2f41outSTOP OUTPUT W
  <L [2]
    <A  'STOP'>
    <L [0]>
  >.


S2F41 = s2f41outSTART OUTPUT W
  <L [2]
    <A  'START'>
    <L [0]>
  >.
  S2F41  = s2f41out OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L[1]
        <L[2]
        <A[MAX 16]> = Commandparametername
        <A[MAX 16]> = Commandparametervalue
        >
    >
>.
S2F41  = s2f41outOtherCommand OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L[0]
    >
>.
S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP-SELECT'> 
    <L[1]
        <L[2]
            <A 'PPID'> 
            <A [MAX 50]> = PPID
        >
    >
>.
S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L [0]       
    >
>.
  

****************************************************************************
* S2F42   Host Command Acknowledge (HCA)                       E -> H      *
****************************************************************************
  
 S2F42 INPUT = s2f42in
    <L [2]
      <B [1] >                     = HCACK
      <L [0] > 
  >.

****************************************************************************
* S5F1   Alarm Report Send (ARS)                               H <- E      *
****************************************************************************

S5F1 = s5f1in     INPUT
 <L [3]
    <B [1] >                            = AlarmCode
    <v>                            		= AlarmId
    <A [MAX 256]>                       = AlarmText
 >.

****************************************************************************
* S5F2   Alarm Report Acknowledge (ARA)                        H -> E      *
****************************************************************************

S5F2 = s5f2out      OUTPUT
  <B [1] >                               = Ackc5.

****************************************************************************
* S6F5   Multi-Block Data Send Inquire (MBI)                   E -> H      *
****************************************************************************

S6F5 OUTPUT = s6f5out
  <L [2]
    <U4 [1] >                    = DataId
    <U4 [1] >                    = DataLength
  >.
  
S6F5 INPUT = s6f5in
  <L [2]
    <U4 [1] >                    = DataId
    <U4 [1] >                    = DataLength
  >.

****************************************************************************
* S6F6   Multi-Block Grant (MBG)                               H -> E      *
****************************************************************************

S6F6 INPUT = s6f6in
  <B [1] >                       = GrantCode.
S6F6 OUTPUT = s6f6out
  <B [1] >                       = GrantCode.

****************************************************************************
* S6F11  Event Report Send (ERS)                               E -> H      *
****************************************************************************  
S6F11 W = s6f11inStripMapUpload  INPUT
<L [3]
       <U4 [1]>                              = DataID
       <U4 [1]>                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1]>                          = ReportID
           <L [1]
             <A [MAX 100000]>                 = MapData
           >                    
         >
       >
 >.

S6F11 W = s6f11EquipStatusChange INPUT
<L [3]
	<U4 [1]>                                   =DataID
	<U4 [1]>                                   =CollEventID
	<L [2]
		<L [2]
			<U4 [1]>                   =ReportID
			<L [2]
				<A[MAX 60]>        =DATE1
				<I2 [1]>   =PreEquipStatus
			>
		>
		<L [2]
			<U4 [1]>                   =ReportID
			<L [2]
				<A[MAX 60]>        =DATE2
				<I2 [1]>   =EquipStatus
			>
		>
	>
>.

S6F11 W = s6f11ControlStateChange INPUT
<L [3]
	<U4 [1]>    =DataID
	<U4 [1]>    =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>   =ReportID
			<L [2]
				<A[MAX 60]>   =Date
				<I2 [1]>      =ControlState
			>
		>
	>
>.
S6F11 = s6f11LoginUserChange  INPUT W
<L [3]
	<U4[1] > = DataId 
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [1]
				<A[MAX 30] >= UserLoginName
			>
		>
	>
>.
S6F11 = s6f11PPExecName  INPUT W
<L [3]
	<U4[1] > = DataId 
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [2]
				<A[MAX 40]>= Clock
				<A[MAX 100]>= PPExecName

			>
		>
	>
>. 
  S6F11 = s6f11incommon1  INPUT W
<L [3]
	<U1[1] > =  DataId
	<U2[1] >= CollEventID
	<L [0]
	>
>. 
S6F11 W = s6f11incommon2 INPUT
<L [3]
	<U4 [1]>                                   =DataID
	<U4 [1]>                                   =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>    =ReportID
			<L [1]
			 <I2 [1]>   =ControlState
			>
		>
	>
>.

S6F11 = s6f11incommon3  INPUT W
<L [3]
	<U4[1] > = DataId
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [3]
				<A[MAX 20] > = Time
				<I2[1] > = Data1
				<I2[1] > = Data2
			>
		>
	>
>.
S6F11 W = s6f11incommon4 INPUT
<L [3]
	<U4 [1]>    =DataID
	<U4 [1]>    =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>                                 =ReportID
			<L [2]
				<I2 [1]>                        =Data2
                                <I2 [1]>                        =Data3
			>
		>
	>
>. 

S6F11 W = s6f11incommon5 INPUT
<L [3]
	<U4 [1]>    =DataID
	<U4 [1]>    =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>                                 =ReportID
			<L [6]
				<A[MAX 60]>   = Data1
                                <U2 [1]>      = Data2
                                <A[MAX 60]>   = Data3
                                <A[MAX 60]>   = Data4
                                <A[MAX 60]>   = Data5
                                <A[MAX 60]>   = Data6
			>
		>
	>
>. 

S6F11 W = s6f11incommon6 INPUT
<L [3]
	<U4 [1]>    =DataID
	<U4 [1]>    =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>                                 =ReportID
			<L [3]
				<A[MAX 60]>   = Data1
                                <U2 [1]>      = Data2
                                <A[MAX 60]>   = Data3
			>
		>
	>
>.

S6F11 W = s6f11incommon7 INPUT
<L [3]
	<U4 [1]>    =DataID
	<U4 [1]>    =CollEventID
	<L [1]
		<L [2]
			<U4 [1]>                                 =ReportID
			<L [5]
				<A[MAX 60]>   = Data1
                                <A[MAX 60]>   = Data3
                                <A[MAX 60]>   = Data3
                                <A[MAX 60]>   = Data3
                                <U4 [1]>      = Data2                                
			>
		>
	>
>.


S6F11 = s6f11incommon8  INPUT W
<L [3]
	<U4[1] >= DataId
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] >= ReportId
			<L [1]
				<I4[1] >= Data1
			>
		>
	>
>.  

S6F11 = s6f11incommon9  INPUT W
<L [3]
	<U4[1] > = DataId 
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
		 <U4[1] > = ReportId
		  <L [1]
		   <U1[1] 1>= Data1
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
* S6F15   Event Report Request (ERR)                           H -> E      *
****************************************************************************

S6F15 = s6f15out OUTPUT W
<U1 [2]> = CollEventId.

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
S7F1  = s7f1multiout OUTPUT W
    <V> = VALUE.
****************************************************************************
* S7F2   Process Program Load Grant                          E<->H      *
****************************************************************************
S7F2 OUTPUT = s7f2out
<B [1]> = PPGNT.

S7F2 INPUT = s7f2in
<B [1]> = PPGNT.

****************************************************************************
* S7F3   Process Program Send                                E<->H      *
****************************************************************************
S7F3  = s7f3out OUTPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                     =Processprogram
>.
S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                   =Processprogram
>.
S7F3  = s7f3multiout OUTPUT W
   <V> = VALUE.
****************************************************************************
* S7F4   Process Program Acknowledge                          E<->H      *
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
S7F6 = s7f6in INPUT 
<L[2]
    <A [MAX 100] >               =  ProcessprogramID 
    <V>                     = Processprogram
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
    <V>   = EPPD.
****************************************************************************
****************************************************************************
* S9F1  Unrecognized Device Id (UDN)                           E -> H      *
****************************************************************************

S9F1 INPUT = s9f1input
  <B [10] >                      = BadDevHead.

****************************************************************************
* S9F3  Unrecognized Stream Type (USN)                         E -> H      *
****************************************************************************
S9F3 INPUT = s9f3input
  <B [10] >                      = BadStreamHead.

****************************************************************************
* S9F5  Unrecognized Function Type (UFN)                       E -> H      *
****************************************************************************
S9F5 INPUT = s9f5input
  <B [10] >                      = BadFuncHead.

****************************************************************************
* S9F7  Illegal Data (IDN)                                     E -> H      *
****************************************************************************
 S9F7 INPUT = s9f7input
  <B [10] >                      = IllDataHead.

****************************************************************************
* S9F9  Transaction Timer Timeout (TTN)                        E -> H      *
****************************************************************************
 S9F9 INPUT = s9f9input
  <B [10] >                      = TranTOHead.

****************************************************************************
* S9F11  Data Too Long (DLN)                                   E -> H      *
****************************************************************************
S9F11 INPUT = s9f11input
  <B [10] >                      = DataLongHead.
  
****************************************************************************
* S9F13  Conversation Time Out                                 E -> H      *
****************************************************************************
S9F13 INPUT = s9f13input
  <L[2]
      <v>                      = MEXP
      <v>                      = EDID
  >.

****************************************************************************
* S14F1  GetAttr Request (GAR)                               H  <-  E      *
****************************************************************************
S14F1  INPUT = s14f1in W
<L [5]
	<A ''>
	<A 'Substrate'>
	<L [1]
		<A [MAX 80]>     = StripId
	>
	<L [1]
		<L [3]
			<A 'SubstrateType'>
			<A 'Strip'>
			<U1 0>
		>
	>
	<L [1]
		<A 'MapData'>
	>
>.
****************************************************************************
* S14F2  GetAttr Data (GAD)                                   H <-> E      *
****************************************************************************
S14F2 = s14f2out OUTPUT
<L [2]
    <L [1]
      <L [2]
        <A [MAX 80]>              = StripId
        <L [1]
        	<L[2]
        	   <A  'MapData'> 
	           <A  [MAX 100000]>    = MapData
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
	         <A  [MAX 7000]>    = MapData
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
* S10F1  Terminal request                                     E -> H      *
****************************************************************************
S10F1  = s10f1in  INPUT W
<L [2]
        <V>      = TID
        <A [MAX 200]>   = TEXT
>.
S10F2 output = s10f2out
 <B [1]>        = AckCode
.
 
S10F3 output = s10f3out W
<L [2]
        <B[1]>      = TID
        <A [MAX 200]>   = TEXT
>.

S10F4 INPUT = s10f4in
 <B [1]>        = AckCode
. 