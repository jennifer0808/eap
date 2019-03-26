****************************************************************************
*  Filename    : ICOST340.SML
*  Description : Pfile for the HOST side of the ASM AD838/AD830
*  Author      : Stephen Zhou
*  Date        : 6/26/2016
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

S2F0 = s2f0out OUTPUT.

S2F0 = s2f0in INPUT.

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

S1F2 = s1f2out OUTPUT
  <L [2]
    <A [MAX 32] >               = Mdln
    <A [MAX 32] >               = SoftRev
  >.

S1F2 = s1f2in INPUT
  <L [2]
      <A [MAX 32] >               = Mdln
      <A [MAX 32] >               = SoftRev
  >.
  
  S1F2 = s1f2out838 OUTPUT
  <L [0]
  >.
****************************************************************************
* S1F3    Select Equipment Status Request                      E <- H     *
****************************************************************************
S1F3 =s1f3out OUTPUT W
  <L [0]>.
S1F3 =s1f3singleout OUTPUT W
<L [1]
    <U4 [1]> = SVID
>.

S1F3 = s1f3statecheck OUTPUT W
 <L [3]
    <U4 [1]> = EquipStatus
    <U4 [1]> = PPExecName
    <U4 [1]> = ControlState
>.
S1F3 = s1f3Specific OUTPUT W
<V>=DATA.

S2F13 = s2f13Specific OUTPUT W
       <V>=DATA.
****************************************************************************
* S1F4    Selected Equipment Status                       E -> H     *
****************************************************************************

S1F4 =s1f4in INPUT 
<V> = RESULT.
 
****************************************************************************
* S1F11    Status Values Namelists Request  (svnr)              E <- H     *
****************************************************************************
S1F11 = s1f11out OUTPUT W
  <L[0]>.

****************************************************************************
* S1F12    Status Values Namelists Reply  (svnrr)                   E ->H  *
****************************************************************************
S1F12 = s1f12in INPUT
<V>=RESULT.     
  

****************************************************************************
* S1F13   Establish Communications Request (CR)                H <-> E     *
****************************************************************************

S1F13 =s1f13in INPUT W
  <L [2]
    <A [MAX 32] >                     = Mdln
    <A [MAX 32] >                     = SoftRev
  >.
  
*S1F13 = s1f13out OUTPUT W
* <L [0]>.

S1F13 = s1f13out OUTPUT W
<L [2]
   <A [MAX 32] >                     = Mdln
   <A [MAX 32] >                     = SoftRev
>.

****************************************************************************
* S1F17   Request ON-LINE                H -> E     *
****************************************************************************
S1F17 = s1f17out OUTPUT W.
S1F18 = s1f18in INPUT
  <B [1] > = AckCode.
****************************************************************************
* S1F15   Request OFF-LINE                H -> E     *
****************************************************************************
S1F15 = s1f15out OUTPUT W.
S1F16 =s1f16in INPUT
    <B [1]>=OFLACK.

****************************************************************************

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
      <B [1] >                     = AckCode
      <L [0] >
  >.

 S1F14 = s1f14inAMS INPUT
  <L [2]
    <B [1] >                          = AckCode
    <L [2]
      <A [MAX 32] >                    = Mdln
      <A [MAX 32] >                    = SoftRev
    >
  >.

****************************************************************************
* S2F13    Equipment Constant Request                          H -> E      *
****************************************************************************

S2F13 =s2f13out OUTPUT W
< L [0]>.

****************************************************************************
* S2F14    Equipment Constant Data                          H<- E      *
****************************************************************************

S2F14 =s2f14in INPUT 
< V >=RESULT.

****************************************************************************
* S2F15   New Equipment Constant Send (ECS)                      H  -> E   *
****************************************************************************
S2F15 = s2f15out OUTPUT  W
  <L [2]
     <L[2]
         <U2[1]>                        = EC096
         <A[MAX 60] >                = NextLotId
     >
     <L[2]
         <U2 [1]>                        = EC097
         <U4 [1]>                        = NextLotQuantity
     >
  >.

S2F16 = s2f16in   INPUT
  <B [1]>                       = EAC.
****************************************************************************
* S2F29   Equipment Canstant Namelist Request                   H- >E      *
****************************************************************************

S2F29 =s2f29out OUTPUT W 
< L [0]>.
S2F29 =s2f29oneout OUTPUT W 
< L [1]
    <U4[1]>=ECID
>.

****************************************************************************
* S2F30     Equipment Canstant Namelist Reply                    H- >E      *
****************************************************************************

S2F30 =s2f30in INPUT 
<V> = RESULT.
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
****************************************************************************
* S2F38  Enable / Disable Event Report Acknowledge (EERA)      E -> H      *
****************************************************************************

S2F38 INPUT = s2f38in 
<B [1]> = AckCode.

****************************************************************************
* S2F41   Host Command Send (HCS)                              H -> E      *
****************************************************************************

S2F41  = s2f41zeroout OUTPUT W
<L[2]
    <A[MAX 80]> = Remotecommand
    <L[0]
    >
>.
S2F41  = s2f41out OUTPUT W
<L[2]
    <A [MAX 10]> =Remotecommand
    <L[1]
        <L[2]
            <A[4] > = Commandparametername
            <A [MAX 50]> = Commandparametervalue
        >
    >
>.

S2F41  = s2f41outPPSelect OUTPUT W
<L[2]
    <A 'PP-SELECT'> 
    <L[1]
        <L[2]
            <A 'PPID'> 
            <A [MAX 100]> = PPID
        >    
    >
>.
S2F41  = s2f41outstart OUTPUT W
<L[2]
    <A 'START'> 
        <L[6]
            <L[2]
                <A 'BATCH-NAME'>
                <A [MAX 100]> = BatchName
                >
            <L[2]
                <A 'ACTION'>
                <A 'NEW'> 
                >
            <L[2]
                <A 'BATCH-TO-PROCESS'> 
                <A ''>
                >
            <L[2]
                <A 'CARRIER-COUNT'> 
                <A ''> 
                >
            <L[2]
                <A 'INPUT-TRAY-MAP'>
                <A ''> 
                >
            <L[2]
                <A 'TRAY-REPORTING'> 
                <A 'NO'>
                >
        >
>.
****************************************************************************
* S2F42   Host Command Acknowledge (HCA)                       E -> H      *
****************************************************************************
S2F42 = s2f42in INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[1]
        <L[2]
            <A [MAX 50]> = PARAMETERNAME
            <B [1] > = CPACK
        >
    >
>.
S2F42 = s2f42zeroin INPUT 
<L [2]
    <B [1] > = HCACK 
    <L[0]
    >
>.
****************************************************************************
* S5F1   Alarm Report Send (ARS)                               H <- E      *
****************************************************************************

S5F1 = s5f1in   INPUT W
 <L [3]
    <B [1] > = ALCD
    <U4 [1] > = ALID
    <A [MAX 100]> = ALTX
 >.

****************************************************************************
* S5F2   Alarm Report Acknowledge (ARA)                        H -> E      *
****************************************************************************

S5F2 = s5f2out      OUTPUT
  <B [1] >   = AckCode.
****************************************************************************
* S5F3  Enable Alarm Send                    E <- H      *
****************************************************************************
S5F3 OUTPUT = s5f3allout  W 
<L [2]
    < B [1] > = ALED 
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
S6F11 =s6f11equipstate INPUT W 
<L [3]
	<U4 [1] > = DataID
	<U4 [1] > = CollEventID	
	<L [0]
	>
>.

S6F11 =s6f11equipstatuschange INPUT W 
<L [3]
	<U4[1] >  = DataID
	<U4[1] >   = CollEventID
	<L [1]
		<L [2]
			<U4[1] >   = RPTID
			<L [2]
				<U1[1] >   = PreStatus
				<U1[1] >   = EquipStatus
			>
		>
	>
>.


S6F11 = s6f11incommon  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <F4 [1]>                     =Data1
             <U4 [1]>                     =Data2
             <U4 [1]>                     =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon1  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <I2 [1] >                        =Data2
           >                    
         >
       >
 >.

S6F11 = s6f11incommon2  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
             <I2 [1] >                        =Data2
             <I2 [1] >                        =Data3
           >                    
         >
       >
 >.
S6F11 = s6f11incommon3  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <U1 [1] >                        =Data2
           >                    
         >
       >
 >.


S6F11 = s6f11incommon5  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [1]
             <I2 [1] >                     =Data1
           >                    
         >
       >
 >.

S6F11 = s6f11incommon6  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
             <A [MAX 60]>                     =Data2
             <A [MAX 60]>                     =Data3
           >                    
         >
       >
 >.

S6F11 = s6f11incommon7  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [4]
             <U4 [1]>                         =Data2
	     <U1 [1]>                         =Data3
	     <A [MAX 60]>                     =Data1
	     <L [0]
	     >
           >                    
         >
       >
 >.

S6F11 = s6f11incommon8  INPUT W
<L [3]
	<U4 [1]> = DataID
	<U4 [1]>  = CollEventID
	<L [2]
		<L [2]
			<U4 [1]>   = Data5
			<L [2]
				<A [MAX 60]>                     =Data1
				<A [MAX 60]>                     =Data2
			>
		>
		<L [2]
			<U4 [1]> = Data6
			<L [2]
				<A [MAX 60]>                     =Data3
				<A [MAX 60]>                     =Data4
			>
		>
	>
>. 
S6F11 = s6f11incommon9 INPUT W 
<L [3]
	<U4[1] > = DataID
	<U4[1] > = CollEventID
	<L [1]
		<L [2]
			<U4[1] > =ReportId
			<L [4]
				<A[MAX 60] > = Data1
				<A[MAX 60] > = Data2
				<V> = Data3
				<U4 [1] > = Data4
			>
		>
	>
>. 
  S6F11 = s6f11incommon10  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [2]
             <A [MAX 60]>                     =Data1
             <A [MAX 60]>                     =Data2         
           >                    
         >
       >
 >.
  S6F11 = s6f11incommon11  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [3]
             <A [MAX 60]>                     =Data1
            <U4 [1] >                        = Data2
             <A [MAX 60]>                     =Data3         
           >                    
         >
       >
 >.
S6F11 = s6f11incommon13  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
           <L [4]
             <A  [MAX 50]>                    = TimeStr
             <I2 [1] >                        = Data10
             <I2 [1] >                        = Data11
	     <A [0] >
           >                    
         >
       >
 >.
S6F11 = s6f11incommon14  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
			<L [13]
				<B[1]>=Data1
				<B[1]>=Data2
				<B[1]>=Data3
				<B[1]>=Data4
				<B[1]>=Data5
				<B[1]>=Data6
				<B[1]>=Data7
				<B[1]>=Data8
				<B[1]>=Data9
				<A[MAX 100] >=Data10
				<U4[1] >=Data11
				<U4[1] >=Data12
				<U4[1] >=Data13
			>
		>
	>
>. 
S6F11 = s6f11incommon15  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
			<L [10]
				<A[MAX 100] >=DATA01
				<A[MAX 100]>=DATA02
				<A[MAX 100]>=DATA03
				<A[MAX 100]>=DATA04
				<U4[1] >=DATA05
				<U4[1] >=DATA06
				<U4[1] >=DATA07
				<U4[1] >=DATA08
				<U4[1] >=DATA09
				<V>=DATA010
			>
		>
	>
>. 
S6F11 = s6f11incommon16  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
			<L [13]
				<B[0]>=DATA0
				<B[0]>=DATA1
				<B[0]>=DATA2
				<B[0]>=DATA3
				<B[0]>=DATA4
				<B[0]>=DATA5
				<B[0]>=DATA6
				<B[0]>=DATA7
				<B[0]>=DATA8
				<A[MAX 100] >=Data10
				<U4[1] >=Data11
				<U4[1] >=Data12
				<U4[1] >=Data13
			>
		>
	>
>.
S6F11 = s6f11incommon17  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
			<L [11]
				<A[MAX 100]>=DATA01
				<A[MAX 100]>=DATA02
				<A[MAX 100]>=DATA03
				<A[MAX 100]>=DATA04
				<U4[1] >=DATA05
				<U4[1] >=DATA06
				<U4[1] >=DATA07
				<U4[1] >=DATA08
				<U4[1] >=DATA09
				<V>=DATA10
				<V>=DATA11
			>
		>
	>
>.
S6F11 = s6f11incommon18 INPUT W
<L [3]
	<U2[1] > =DataID
	<U4[1] > =CollEventID
	<L [1]
		<L [2]
			<U4[1] > = ReportId
			<L [0]
			>
		>
	>
>.  
S6F11 = s6f11incommon19 INPUT W
<L [3]
	<U2[1] > =DataID
	<U4[1] > =CollEventID
	<L [0]
	>
>.
S6F11 = s6f11incommon20  INPUT W
<L [3]
       <U4 [1] >                              = DataID
       <U4 [1] >                              = CollEventID
       <L [1]
         <L [2]
           <U4 [1] >                          = ReportId
			<L [9]
				<V>=data0
				<V>=data1
				<V>=data2
				<V>=data3
				<V>=data4
				<V>=data5
				<V>=data6
				<V>=data7
				<V>=data8
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
* S7F1    Process Program Load Inquire                          E<->H      *
****************************************************************************

S7F1  = s7f1out OUTPUT W
<L[2]
    <A [MAX 100] >             = ProcessprogramID
    <U4 [1]>                   = Length
>.

S7F1  = s7f1multiout OUTPUT W
<L[3]
<L[2]
    <A [MAX 100] >             = ProcessprogramID0
    <U4 [1]>                   = Length0
>
<L[2]
    <A [MAX 100] >             = ProcessprogramID1
    <U4 [1]>                   = Length1
>
<L[2]
    <A [MAX 100] >             = ProcessprogramID2
    <U4 [1]>                   = Length2
>
>.

S7F1  = s7f1in INPUT W
<L[2]
    <A [MAX 100] >               = ProcessprogramID
    <U4 [1]>                    = Length
>.

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

S7F3  = s7f3multiout OUTPUT W
<L[3]
<L[2]
    <A [MAX 100] >               = ProcessprogramID0
    <V>                   =Processprogram0
>
<L[2]
    <A [MAX 100] >               = ProcessprogramID1
    <V>                   =Processprogram1
>
<L[2]
    <A [MAX 100] >               = ProcessprogramID2
    <V>                   =Processprogram2
>
>.

S7F3 INPUT =s7f3in W
<L[2]
    <A [MAX 100] >               = ProcessprogramID 
    <V>                   =Processprogram
>.

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
    <V>=EPPD. 
****************************************************************************
* S10F1  Terminal request                                     E -> H      *
****************************************************************************
S10F1 output = s10f1out W
<L [2]
        <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F1  = s10f1in  INPUT W
<L [2]
        <V>      = TID
        <A [MAX 1000]>   = TEXT
>.
S10F2 INPUT = s10f2in
 <B [1]>        = AckCode.

  S10F2 OUTPUT = s10f2out
 <B [1]>        = AckCode.

S10F3 output = s10f3out W
<L [2]
        <B[1]>      = TID
        <A [MAX 1000]>   = TEXT
>.

S10F4 INPUT = s10f4in
 <B [1]>        = AckCode.